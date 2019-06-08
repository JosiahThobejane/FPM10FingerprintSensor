package za.co.josiahthobejane.fingerprint.DeleteModel;

import jssc.SerialPortException;
import sk.upjs.zirro.fpm10sensor.FingerprintSensor;
import sk.upjs.zirro.fpm10sensor.FingerprintSensorException;

public class DeleteModel {
    public static void main(String[] args) {
        FingerprintSensor sensor = new FingerprintSensor("COM14");
        sensor.open(); // perform a handshake with the sensor
        
        try {
            //deletes fingerprint template at INDEX 2 and gives the delete process a timeout of 2 seconds.                      
            sensor.deleteModel(2, 2000);                  
            System.out.println("Delete success. \n" 
                    + "Current Models in Storage: " + sensor.getTemplateCount(1000) + "/" + sensor.getLibraryCapacity());
            sensor.close();
        } catch (FingerprintSensorException | SerialPortException e) {            
            e.printStackTrace();
            sensor.close();
        }

    }
}