package tijos.framework.sensor.ht1621;

import java.io.IOException;
import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.util.Delay;

/**
 * HT1621 6-Segment LCD Sample
 *
 */
public class TiHT1621Sample {
	public static void main(String[] args) {
		System.out.println("Hello World!");

		try {
			
			/**
			 * GPIO Port 0
			 */
			int gpioPort = 0;
			
			/**
			 * GPIO Pin 
			 */
			int csPin = 0;
			int wrPin = 1;
			int datPin = 2;
			int backlightPin = 3;

			TiGPIO gpio = TiGPIO.open(gpioPort, csPin, wrPin, datPin, backlightPin);

			TiHT1621 ht1621 = new TiHT1621(gpio, csPin, wrPin, datPin, backlightPin);

			ht1621.initialize();

			float value = 123.456f;
			while (true) {
				
				
				ht1621.showBattaryTop(false);
				ht1621.showBatteryMiddle(true);
				ht1621.showBatteryBottom(true);
				
				value += 0.1f;
				
				ht1621.displayNumber(value);
		 
				Delay.msDelay(2000);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
