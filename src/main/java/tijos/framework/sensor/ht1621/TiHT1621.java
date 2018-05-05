package tijos.framework.sensor.ht1621;

import java.io.IOException;

import tijos.framework.devicecenter.TiGPIO;

/**
 * HT1621 6-Segment LCD driver for TiJOS based on https://github.com/anxzhu/segment-lcd-with-ht1621
 * 
 */
public class TiHT1621 {

	private static final int BIAS = 0x52; // 0b1000 0101 0010 1/3duty 4com
	private static final int SYSDIS = 0X00; // 0b1000 0000 0000 关振系统荡器和LCD偏压发生器
	private static final int SYSEN = 0X02; // 0b1000 0000 0010 打开系统振荡器
	private static final int LCDOFF = 0X04; // 0b1000 0000 0100 关LCD偏压
	private static final int LCDON = 0X06; // 0b1000 0000 0110 打开LCD偏压
	private static final int XTAL = 0x28; // 0b1000 0010 1000 外部接时钟
	private static final int RC256 = 0X30; // 0b1000 0011 0000 内部时钟
	private static final int TONEON = 0X12; // 0b1000 0001 0010 打开声音输出
	private static final int TONEOFF = 0X10; // 0b1000 0001 0000 关闭声音输出
	private static final int WDTDIS1 = 0X0A; // 0b1000 0000 1010 禁止看门狗

	byte[] battery = new byte[3];

	int _csPin;
	int _wrPin;
	int _datPin;
	int _backlightPin;

	TiGPIO gpio;

	public TiHT1621(TiGPIO gpioPort, int csPin, int wrPin, int datPin, int backlightPin) throws IOException {
		this.gpio = gpioPort;
		_csPin = csPin;
		_wrPin = wrPin;
		_datPin = datPin;
		_backlightPin = backlightPin;

		gpio.setWorkMode(_csPin, TiGPIO.OUTPUT_PP);
		gpio.setWorkMode(_wrPin, TiGPIO.OUTPUT_PP);
		gpio.setWorkMode(_datPin, TiGPIO.OUTPUT_PP);
		gpio.setWorkMode(_backlightPin, TiGPIO.OUTPUT_PP);

	}

	public void turnOnLCD() throws IOException {
		wrCMD(LCDON);
	}

	public void turnOffLCD() throws IOException {
		wrCMD(LCDOFF);
	}

	public void turnOnBackLight() throws IOException {
		gpio.writePin(_backlightPin, 1);
	}

	public void turnOffBackLight() throws IOException {
		gpio.writePin(_backlightPin, 0);
	}

	/**
	 * Initialize 
	 * @throws IOException
	 */
	public void initialize() throws IOException {
		wrCMD(BIAS);
		wrCMD(RC256);
		wrCMD(SYSDIS);
		wrCMD(WDTDIS1);
		wrCMD(SYSEN);
		wrCMD(LCDON);

	}

	private void wrDATA(int data, int cnt) throws IOException {
		for (int i = 0; i < cnt; i++) {
			gpio.writePin(_wrPin, 0);
			if ((data & 0x80) > 0) {
				gpio.writePin(_datPin, 1);
			} else {
				gpio.writePin(_datPin, 0);
			}
			gpio.writePin(_wrPin, 1);
			data <<= 1;
		}
	}

	private void wrclrdata(int addr, int sdata) throws IOException {
		addr <<= 2;
		gpio.writePin(_csPin, 0);
		wrDATA(0xa0, 3);
		wrDATA(addr, 6);
		wrDATA(sdata, 8);
		gpio.writePin(_csPin, 1);
	}

	private void wrone(int addr, int sdata) throws IOException {

		addr <<= 2;
		gpio.writePin(_csPin, 0);
		wrDATA(0xa0, 3);
		wrDATA(addr, 6);
		wrDATA(sdata, 8);
		gpio.writePin(_csPin, 1);
	}

	private void wrCMD(int CMD) throws IOException { // 100
		gpio.writePin(_csPin, 0);
		wrDATA(0x80, 4);
		wrDATA(CMD, 8);
		gpio.writePin(_csPin, 1);
	}

	private void wrCLR(int len) throws IOException {
		int addr = 0;
		for (int i = 0; i < len; i++) {
			wrclrdata(addr, 0x00);
			addr = addr + 2;
		}
	}

	/**
	 * Battery segment 0 
	 * @param on display or not
	 */
	public void showBattaryTop(boolean on) { 
		if(on)
			battery[0] = (byte) 0x80;
		else
			battery[0] = 0x00; 
	}

	/**
	 * Battery segment 1 
	 * @param on display or not
	 */
	public void showBatteryMiddle(boolean on) { // 电池中
		if(on)
			battery[1] = (byte) 0x80;
		else
			battery[1] = 0x00;
	}

	/**
	 * Battery segment 2 
	 * @param on display or not
	 */
	public void showBatteryBottom(boolean on) { // 电池底
		if(on)
			battery[2] = (byte) 0x80;
		else
			battery[2] = 0x00; 
	}

	/**
	 * clear screen
	 * @throws IOException
	 */
	public void clear() throws IOException {
		wrCLR(16);
	}

	private void display(byte addr, byte sdata) throws IOException {
		wrone(addr, sdata);
	}
	
	/**
	 * Display number and battery, range from 0.001 - 99999.9
	 * @param num number to be displayed
	 * @throws IOException
	 */
	public void displayNumber(float num) throws IOException {
		String strFloat  = Float.toString(num);
		byte[] strBuff = strFloat.getBytes();
				
		byte[] buffer =new byte[7];
		int len = strFloat.length();
		if(len > buffer.length)
			len = buffer.length;
		
		System.arraycopy(strBuff, 0, buffer, 0, len);

		int dpposition;
		dpposition = strFloat.indexOf('.');// 寻找小数点位置 取前七位 因为最多显示七位

		// 为6 整数 如123456.
		// 5 一位小数 12345.6
		// 4 两位小数 1234.56
		// 3 三位小数 123.456
		// 2 三位小数 12.345
		// 1 三位小数 1.234
		// unsigned char
		// lednum[10]={0x7D,0x60,0x3E,0x7A,0x63,0x5B,0x5F,0x70,0x7F,0x7B};//显示 0 1 2 3 4
		// 5 6 7 8 9

		for (int i = 0; i < buffer.length; i++) {
			switch (buffer[i]) {
			case '0':
				buffer[i] = 0x7D;
				break;
			case '1':
				buffer[i] = 0x60;
				break;
			case '2':
				buffer[i] = 0x3e;
				break;
			case '3':
				buffer[i] = 0x7a;
				break;
			case '4':
				buffer[i] = 0x63;
				break;
			case '5':
				buffer[i] = 0x5b;
				break;
			case '6':
				buffer[i] = 0x5f;
				break;
			case '7':
				buffer[i] = 0x70;
				break;
			case '8':
				buffer[i] = 0x7f;
				break;
			case '9':
				buffer[i] = 0x7b;
				break;
			case '.':
				buffer[i] = (byte) 0xff;
				break;
			}
		}

		switch (dpposition) {
		case 6:
			wrone(0, buffer[5]);// 123456.
			wrone(2, buffer[4]);
			wrone(4, buffer[3]);
			wrone(6, buffer[2] | battery[2]);
			wrone(8, buffer[1] | battery[1]);
			wrone(10, buffer[0] | battery[0]);
			break;
		case 5:
			wrone(0, (buffer[6] | 0x80));// 12345.6
			wrone(2, buffer[4]);
			wrone(4, buffer[3]);
			wrone(6, buffer[2] | battery[2]);
			wrone(8, buffer[1] | battery[1]);
			wrone(10, buffer[0] | battery[0]);
			break;
		case 4:
			wrone(0, buffer[6]);// 1234.56
			wrone(2, (buffer[5] | 0x80));
			wrone(4, buffer[3]);
			wrone(6, buffer[2] | battery[2]);//
			wrone(8, buffer[1] | battery[1]);
			wrone(10, buffer[0] | battery[0]);
			break;
		case 3:
			wrone(0, buffer[6]);// 123.456
			wrone(2, buffer[5]);
			wrone(4, (buffer[4] | 0x80));
			wrone(6, buffer[2] | battery[2]);
			wrone(8, buffer[1] | battery[1]);
			wrone(10, buffer[0] | battery[0]);
			break;

		case 2:
			wrone(0, buffer[5]);// 12.345
			wrone(2, buffer[4]);
			wrone(4, (buffer[3] | 0x80));
			wrone(6, buffer[1] | battery[2]);
			wrone(8, buffer[0] | battery[1]);
			wrone(10, 0x00 | battery[0]);
			break;
		case 1:
			wrone(0, buffer[4]);// 1.234
			wrone(2, buffer[3]);
			wrone(4, (buffer[2] | 0x80));
			wrone(6, buffer[0] | battery[2]);
			wrone(8, 0x00 | battery[1]);
			wrone(10, 0x00 | battery[0]);

			break;
		default:
			break;

		}
	}

}
