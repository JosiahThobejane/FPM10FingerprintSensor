package sk.upjs.zirro.fpm10sensor;

import jssc.SerialPort;
import jssc.SerialPortException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class FingerprintSensor {

	/**
	 * The received package.
	 */
	private static final class Package {
		/**
		 * Package type.
		 */
		final int type;

		/**
		 * Payload of the package.
		 */
		final int[] data;

		/**
		 * Constructs a received package.
		 *
		 * @param type
		 *            the package type.
		 * @param data
		 *            package data.
		 */
		Package(int type, int[] data) {
			this.type = type;
			this.data = data;
		}
	}

	/**
	 * The search result.
	 */
	public final class SearchResult {
		/**
		 * Fingerprint id.
		 */
		final int id;

		/**
		 * Matching score.
		 */
		final int matchScore;

		/**
		 * Constructs a search result.
		 *
		 * @param id
		 *            fingerprint id.
		 * @param matchScore
		 *            matching score.
		 */
		SearchResult(int id, int matchScore) {
			this.id = id;
			this.matchScore = matchScore;
		}
	}

	// -------------------------------------------------------------
	// Confirmation codes
	// -------------------------------------------------------------

	/**
	 * Instruction execution is completed or ok.
	 */
	private static final int CC_OK = 0x00;

	/**
	 * Error when receiving data package.
	 */
	private static final int CC_PACKET_RECEIVE_ERR = 0x01;

	/**
	 * No finger on the sensor.
	 */
	private static final int CC_NO_FINGER = 0x02;

	/**
	 * Fail to enroll the finger.
	 */
	private static final int CC_IMAGE_FAIL = 0x03;

	/**
	 * Fail to generate character file due to the over-disorderly fingerprint
	 * image.
	 */
	private static final int CC_IMAGE_MESSY = 0x06;

	/**
	 * Fail to generate character file due to lack of character points.
	 */
	private static final int CC_FEATURE_FAIL = 0x07;

	/**
	 * Fingerprints do not match.
	 */
	private static final int CC_NO_MATCH = 0x08;

	/**
	 * Fail to find matching fingerprint.
	 */
	private static final int CC_MATCH_NOT_FOUND = 0x09;

	/**
	 * Fail to combine the character files.
	 */
	private static final int CC_ENROLL_MISMATCH = 0x0A;

	/**
	 * Addressing page ID is beyond the fingerprint library.
	 */
	private static final int CC_BAD_LOCATION = 0x0B;

	/**
	 * Error when reading template from library or the template is invalid.
	 */
	private static final int CC_READ_TEMP_ERR = 0x0C;

	/**
	 * Error when uploading template.
	 */
	private static final int CC_UPLOAD_TEMP_ERR = 0x0D;

	/**
	 * Module can not receive the following data packages.
	 */
	private static final int CC_PACKET_RESPONSE_ERR = 0x0E;

	/**
	 * Error when uploading image.
	 */
	private static final int CC_UPLOAD_IMAGE_ERR = 0x0F;

	/**
	 * Fail to delete the template.
	 */
	private static final int CC_DELETE_TEMP_FAIL = 0x10;

	/**
	 * Fail to clear finger library.
	 */
	private static final int CC_EMPTY_LIB_FAIL = 0x11;

	/**
	 * Incorrect password.
	 */
	private static final int CC_INCORRECT_PASSWORD = 0x13;

	/**
	 * Fail to generate the image for the lack of valid primary image.
	 */
	private static final int CC_INVALID_IMAGE = 0x15;

	/**
	 * Error when writing to flash
	 */
	private static final int CC_FLASH_ERR = 0x18;

	/**
	 * Invalid register number.
	 */
	private static final int CC_INVALID_REGISTER = 0x1A;

	/**
	 * Wrong address code.
	 */
	private static final int CC_WRONG_ADDRESS = 0x20;

	/**
	 * Must verify the password
	 */
	private static final int CC_VERIFY_PASSWORD = 0x21;

	/**
	 * Incorrect array size.
	 */
	private static final int WRONG_SCAN_SIZE = -2;

	// -------------------------------------------------------------
	// Package types
	// -------------------------------------------------------------
	/**
	 * Command packet.
	 */
	private static final int PACKET_TYPE_COMMAND = 0x01;

	/**
	 * Data packet.
	 */
	private static final int PACKET_TYPE_DATA = 0x02;

	/**
	 * Acknowledge packet.
	 */
	private static final int PACKET_TYPE_ACK = 0x07;

	/**
	 * End of Data packet.
	 */
	private static final int PACKET_TYPE_ENDDATA = 0x08;

	// -------------------------------------------------------------
	// Instruction codes
	// -------------------------------------------------------------

	/**
	 * Collect finger image.
	 */
	private static final int IC_GET_IMAGE = 0x01;

	/**
	 * Generate character file from image.
	 */
	private static final int IC_IMAGE2TZ = 0x02;

	/**
	 * Match two templates.
	 */
	private static final int IC_MATCH = 0x03;

	/**
	 * Search.
	 */
	private static final int IC_SEARCH = 0x04;

	/**
	 * Combine character files from char buffer1 and char buffer2 and generate
	 * template.
	 */
	private static final int IC_CREATE_MODEL = 0x05;

	/**
	 * Store template.
	 */
	private static final int IC_STORE = 0x06;

	/**
	 * Load template.
	 */
	private static final int IC_LOAD_CHAR = 0x07;

	/**
	 * Download characteristics.
	 */
	private static final int IC_DOWNLOAD_CHAR = 0x08;

	/**
	 * Upload characteristics.
	 */
	private static final int IC_UPLOAD_CHAR = 0x09;

	/**
	 * Download image from image buffer to computer.
	 */
	private static final int IC_DOWNLOAD_IMAGE = 0x0A;

	/**
	 * Upload image from computer to image buffer.
	 */
	private static final int IC_UPLOAD_IMAGE = 0x0B;

	/**
	 * Delete characteristics.
	 */
	private static final int IC_DELETE_CHAR = 0x0C;

	/**
	 * Empty database.
	 */
	private static final int IC_EMPTY_LIB = 0x0D;

	/**
	 * Read system parameters.
	 */
	private static final int IC_READ_SYSTEM_PARAM = 0x0F;

	/**
	 * Verify password.
	 */
	private static final int IC_VERIFY_PASSWORD = 0x13;

	/**
	 * Read finger template numbers.
	 */
	private static final int IC_TEMPLATE_COUNT = 0x1D;

	/**
	 * The char file buffer 1.
	 */
	private static final byte CHAR_BUFFER1 = 0x01;

	/**
	 * The char file buffer 2.
	 */
	private static final byte CHAR_BUFFER2 = 0x02;

	/**
	 * The serial port.
	 */
	private final SerialPort serialPort;

	/**
	 * The buffered serial port reader created after the connection is open.
	 */
	private SerialPortReader serialPortReader;

	/**
	 * The name of serial port.
	 */
	private final String serialPortName;

	/**
	 * The baud rate of serial link to the sensor.
	 */
	private int baudRate;

	/**
	 * The password to access the sensor.
	 */
	private long password = 0x00000000;

	/**
	 * The 2-bytes header of each packet.
	 */
	private int packageHeader = 0xEF01;

	/**
	 * The module address.
	 */
	private long moduleAddress = 0xFFFFFFFFL;

	/**
	 * Default timeout for operations in milliseconds.
	 */
	private long defaultTimeout = 2000;

	/**
	 * The capacity of fingerprint Flash library.
	 */
	private int libraryCapacity;

	/**
	 * The parameter controls the UART communication speed of the Module. Its
	 * value is an integer N (1 - 12). Baud rate is 9600*N bps.
	 */
	private int baudRateControl;

	/**
	 * The parameter controls the matching threshold value of fingerprint
	 * searching and matching. Security level is divided into 5 grades (1 - 5).
	 */
	private int securityLevel;

	/**
	 * The current operation status of the Module. 1: system is executing
	 * commands, 0: system is free
	 */
	private int statusRegister;

	/**
	 * The max length of the transferring data package when communicating with
	 * upper computer. Its value is 32 bytes, 64 bytes, 128 bytes, 256 bytes.
	 */
	private int dataPackageLength;

	/**
	 * The system identifier code.
	 */
	private int systemIdentifierCode;

	/**
	 * The prolog of each (sent or received) package.
	 */
	private final int[] packageProlog = new int[6];

	/**
	 * Constructs the fingerprint sensor wrapper with physical sensor connected
	 * via given serial port at specified baud rate.
	 *
	 * @param serialPort
	 *            the serial port.
	 * @param baudrate
	 *            the baud rate of serial port.
	 */
	public FingerprintSensor(String serialPort, int baudrate) {
		this.serialPort = new SerialPort(serialPort);
		this.serialPortName = serialPort;
		this.baudRate = baudrate;
	}

	/**
	 * Constructs the fingerprint sensor wrapper with physical sensor connected
	 * via given serial port at default baud rate.
	 *
	 * @param serialPort
	 *            the serial port.
	 */
	public FingerprintSensor(String serialPort) {
		this(serialPort, SerialPort.BAUDRATE_57600);
	}

	/**
	 * Returns the serial port.
	 * 
	 * @return the serial port.
	 */
	public String getSerialPort() {
		return serialPortName;
	}

	/**
	 * @return the capacity of fingerprint Flash library.
	 */
	public int getLibraryCapacity() {
		return libraryCapacity;
	}

	/**
	 * @return an integer N (1 - 12). Baud rate is 9600*N bps.
	 */
	public int getBaudRateControl() {
		return baudRateControl;
	}

	/**
	 * @return value (1-5) - threshold value of fingerprint searching and
	 *         matching.
	 */
	public int getSecurityLevel() {
		return securityLevel;
	}

	/**
	 * @return the current operation status of the Module.
	 */
	public int getStatusRegister() {
		return statusRegister;
	}

	/**
	 * @return the max length of the transferring data package.
	 */
	public int getDataPackageLength() {
		return dataPackageLength;
	}

	/**
	 * @return the system identifier code.
	 */
	public int getSystemIdentifierCode() {
		return systemIdentifierCode;
	}

	/**
	 * Opens connection to the sensor.
	 */
	public void open() {
		// try open port
		try {
			serialPort.openPort();
			serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			serialPortReader = new SerialPortReader(serialPort, baudRate);
		} catch (Exception e) {
			try {
				serialPortReader = null;
				serialPort.closePort();
			} catch (Exception ignore) {

			}

			throw new FingerprintSensorException("Opening of serial port failed.", e);
		}

		updatePackageProlog();

		// try handshake with sensor
		try {
			if (!verifyPassword(defaultTimeout)) {
				throw new RuntimeException("Handshaking failed.");
			}
			if (!readSystemParameters(defaultTimeout)) {
				throw new RuntimeException("Cannot read system parameters.");
			}

		} catch (Exception e) {
			close();
			throw new FingerprintSensorException("Handshaking with sensor failed.", e);
		}
	}

	/**
	 * Closes connection to the sensor.
	 */
	public void close() {
		if (serialPortReader == null) {
			return;
		}

		try {
			serialPortReader = null;
			serialPort.closePort();
		} catch (Exception ignore) {

		}
	}

	/**
	 * Workflow for uploading an image and searching for a match in the module
	 * library.
	 *
	 * @param imagePathName
	 *            path to the fingerprint image.
	 * @param humanActionListener
	 *            listener for human interaction.
	 * @throws SerialPortException
	 * @throws SerialPortException
	 */
	public SearchResult uploadImageAndSearchWorkflow(String imagePathName, HumanActionListener humanActionListener)
			throws SerialPortException, FingerprintSensorException, IOException {

		BufferedImage image = ImageIO.read(new File(imagePathName));
		int[][] scan = FingerprintUtils.imageToFingerprintScan(image);

		// downloading first scan to image buffer
		uploadImageScan(defaultTimeout, scan);

		// generate char file and store it in char buffer1
		image2Tz(CHAR_BUFFER1, defaultTimeout);

		// return search result
		return search(defaultTimeout);
	}

	/**
	 * Workflow for getting an image from image buffer into computer.
	 *
	 * @param imagePathName
	 * @param humanActionListener
	 * @throws SerialPortException
	 * @throws IOException
	 */
	public void downloadImageWorkflow(String imagePathName, HumanActionListener humanActionListener)
			throws SerialPortException, IOException {
		// instruct human to put finger on the sensor
		humanActionListener.putFinger();
		// wait for a valid fingerprint
		while (!getImage(defaultTimeout))
			;

		// instruct human to remove the finger from sensor
		humanActionListener.removeFinger();

		// get image scan
		int[][] b = getImageScan(defaultTimeout);

		// save picture
		BufferedImage img = FingerprintUtils.fingerprintScanToImage(b);
		ImageIO.write(img, "png", new File(imagePathName));
	}

	/**
	 * Workflow for getting the fingerprint image and saving the template under
	 * the specified id.
	 *
	 * @param fingerprintId
	 *            id of the new template.
	 * @param humanActionListener
	 *            listener for human interaction.
	 * @throws SerialPortException
	 * @throws SerialPortException
	 */
	public void enrollWorkflow(int fingerprintId, HumanActionListener humanActionListener)
			throws SerialPortException, FingerprintSensorException, IOException {
		// instruct human to put finger on the sensor
		humanActionListener.putFinger();
		// wait for a valid fingerprint
		while (!getImage(defaultTimeout))
			;

		// generate char file and store it in char buffer1
		image2Tz(CHAR_BUFFER1, defaultTimeout);

		// instruct human to remove the finger from sensor
		humanActionListener.removeFinger();
		// wait for the finger to be removed
		while (getImage(defaultTimeout))
			;

		try {
			Thread.sleep(2000);
		} catch (InterruptedException exception) {
			return;
		}

		// instruct human to put finger on the sensor
		humanActionListener.putFinger();
		// wait for a valid fingerprint
		while (!getImage(defaultTimeout))
			;

		// generate char file and store it in char buffer2
		image2Tz(CHAR_BUFFER2, defaultTimeout);
		// instruct human to remove the finger from sensor
		humanActionListener.removeFinger();
		// wait for the finger to be removed
		while (getImage(defaultTimeout))
			;

		// generate template by combining char buffer1 and char buffer2, store
		// result back in both buffers
		createModel(defaultTimeout);

		// stores template from specified char buffer in fingerprint library
		storeModel(fingerprintId, CHAR_BUFFER2, defaultTimeout);
	}

	/**
	 * Workflow for searching for a match in the module library.
	 *
	 * @param humanActionListener
	 *            listener for human interaction.
	 * @throws SerialPortException
	 * @throws SerialPortException
	 */
	public SearchResult searchWorkflow(HumanActionListener humanActionListener)
			throws SerialPortException, FingerprintSensorException {
		// instruct human to put finger on the sensor
		humanActionListener.putFinger();
		// wait for a valid fingerprint
		while (!getImage(defaultTimeout))
			;

		// generate char file and store it in char buffer1
		image2Tz(CHAR_BUFFER1, defaultTimeout);

		// instruct human to remove the finger from sensor
		humanActionListener.removeFinger();
		// wait for the finger to be removed
		while (getImage(defaultTimeout))
			;

		// return search result
		return search(defaultTimeout);
	}

	/**
	 * Workflow for comparing fingerprint to specified template in the module
	 * library.
	 *
	 * @param fingerprintId
	 *            id of the template in the module library.
	 * @param humanActionListener
	 *            listener for human interaction.
	 * @throws SerialPortException
	 * @throws SerialPortException
	 */
	public int matchWorkflow(int fingerprintId, HumanActionListener humanActionListener)
			throws SerialPortException, FingerprintSensorException {
		// load model from library to char buffer1
		loadModel(fingerprintId, CHAR_BUFFER1, defaultTimeout);

		// instruct human to put finger on the sensor
		humanActionListener.putFinger();
		// wait for a valid fingerprint
		while (!getImage(defaultTimeout))
			;

		// generate char file and store it in char buffer2
		image2Tz(CHAR_BUFFER2, defaultTimeout);

		// instruct human to remove the finger from sensor
		humanActionListener.removeFinger();
		// wait for the finger to be removed
		while (getImage(defaultTimeout))
			;

		// return match score
		return match(defaultTimeout);
	}

	/**
	 * Verifies the password.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return true, after the password has been verified, false otherwise.
	 */
	private boolean verifyPassword(long timeout) throws SerialPortException {
		// create command data
		int[] commandData = { IC_VERIFY_PASSWORD, 0, 0, 0, 0 };
		long pwd = password;
		for (int i = 4; i >= 1; i--) {
			commandData[i] = (int) (pwd % 256);
			pwd = pwd / 256;
		}

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		return (reply != null) && (reply.type == PACKET_TYPE_ACK) && (reply.data.length == 1)
				&& (reply.data[0] == CC_OK);
	}

	/**
	 * Reads module's status register and system basic configuration parameters.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return true, if parameters have bean read, false otherwise.
	 */
	private boolean readSystemParameters(long timeout) throws SerialPortException {
		// create command data
		int[] commandData = { IC_READ_SYSTEM_PARAM };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);

		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 17)
				|| (reply.data[0] != CC_OK)) {
			return false;
		}
		statusRegister = reply.data[1] * 256 + reply.data[2];
		systemIdentifierCode = reply.data[3] * 256 + reply.data[4];
		libraryCapacity = reply.data[5] * 256 + reply.data[6];
		securityLevel = reply.data[7] * 256 + reply.data[8];
		moduleAddress = (long) reply.data[9] * 256 * 256 * 256 + reply.data[10] * 256 * 256 + reply.data[11] * 256
				+ reply.data[12];
		int dataPackageLengthValue = reply.data[13] * 256 + reply.data[14];
		baudRateControl = (reply.data[15] * 256 + reply.data[16]) * 9600;

		dataPackageLength = 32;
		for (int i = 0; i < dataPackageLengthValue; i++) {
			dataPackageLength *= 2;
		}
		return true;
	}

	/**
	 * Deletes each stored template from the module library.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void emptyModuleLibrary(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int[] commandData = { IC_EMPTY_LIB };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Get the number of stored models in the module library.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return the number of stored models in the module library.
	 */
	public int getTemplateCount(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int[] commandData = { IC_TEMPLATE_COUNT };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive reply package
		Package reply = readPackage(timeout);

		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 3)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}

		int templateCount = reply.data[1] * 256 + reply.data[2];
		return templateCount;
	}

	/**
	 * Search for a match for characteristics stored in char buffer1 in the
	 * module library.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return the result, if the match was found, null otherwise.
	 */
	public SearchResult search(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int[] commandData = { IC_SEARCH, CHAR_BUFFER1, 0, 0, libraryCapacity / 256, libraryCapacity % 256 };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive reply package
		Package reply = readPackage(timeout);

		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 5)) {
			throwFingerprintException(-1);
		} else if ((reply.data[0] != CC_OK) && (reply.data[0] != CC_MATCH_NOT_FOUND)) {
			throwFingerprintException(reply.data[0]);
		}

		if (reply.data[0] == CC_MATCH_NOT_FOUND) {
			return null;
		}
		// return found match
		int fingerId = reply.data[1] * 256 + reply.data[2];
		int matchScore = reply.data[3] * 256 + reply.data[4];
		return new SearchResult(fingerId, matchScore);
	}

	/**
	 * Matches characteristics stored in char buffer1 and char buffer2.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return the matching score if characteristics match, -1 if they do not.
	 */
	public int match(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int[] commandData = { IC_MATCH };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive reply package
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 3)) {
			throwFingerprintException(-1);
		} else if ((reply.data[0] != CC_OK) && (reply.data[0] != CC_NO_MATCH)) {
			throwFingerprintException(reply.data[0]);
		}
		if (reply.data[0] == CC_NO_MATCH) {
			// characteristics in char buffer1 and char buffer2 do not match
			return -1;
		} else {
			// characteristics match, return matching score
			return reply.data[1] * 256 + reply.data[2];
		}
	}

	/**
	 * Deletes specified number of models in fingerprint library starting with
	 * number 'id'.
	 *
	 * @param count
	 *            number of models to delete.
	 * @param id
	 *            the starting pageID of the template to delete.
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void deleteModels(int id, long timeout, int count) throws SerialPortException, FingerprintSensorException {
		// create command data
		int[] commandData = { IC_DELETE_CHAR, id / 256, id % 256, count / 256, count % 256 };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Deletes one model in fingerprint library.
	 *
	 * @param id
	 *            the pageID of the template to delete.
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void deleteModel(int id, long timeout) throws SerialPortException, FingerprintSensorException {
		deleteModels(id, timeout, 1);
	}

	/**
	 * Detects finger and stores the image in image buffer
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return true, if a finger has been detected, false otherwise.
	 */
	public boolean getImage(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int commandData[] = { IC_GET_IMAGE };

		// send packet
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// get reply
		Package reply = readPackage(timeout);

		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if ((reply.data[0] != CC_OK) && (reply.data[0] != CC_NO_FINGER)) {
			throwFingerprintException(reply.data[0]);
		}
		if (reply.data[0] == CC_NO_FINGER) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Generates fingerprint characteristics of the original image in image
	 * buffer and stores the file in char buffer1 or char buffer2
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @param charBufferId
	 *            character file buffer number (1 or 2)
	 */
	private void image2Tz(int charBufferId, long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int packet[] = { IC_IMAGE2TZ, charBufferId };

		// send command
		writePackage(PACKET_TYPE_COMMAND, packet);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Transfers a fingerprint template from chosen char buffer to the computer.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @param charBufferId
	 *            character file buffer number (1 or 2)
	 */
	public int[] downloadModel(int charBufferId, long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int packet[] = { IC_DOWNLOAD_CHAR, charBufferId };

		// send command
		writePackage(PACKET_TYPE_COMMAND, packet);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}

		int[] charBuffer = new int[0];
		int idx = 0;

		while (reply.type != PACKET_TYPE_ENDDATA) {
			reply = readPackage(timeout);
			// increase charBuffer size
			int[] tempBuffer = new int[charBuffer.length + reply.data.length];
			System.arraycopy(charBuffer, 0, tempBuffer, 0, charBuffer.length);
			charBuffer = tempBuffer;

			for (int i = 0; i < reply.data.length; i++) {
				charBuffer[idx] = reply.data[i];
				idx++;
			}
		}
		return charBuffer;
	}

	/**
	 *
	 * Transfers a fingerprint template to the specified char buffer.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @param charBufferId
	 *            character file buffer number (1 or 2)
	 */
	public boolean uploadModel(int charBufferId, int[] model, long timeout)
			throws SerialPortException, FingerprintSensorException {
		// create command data
		int packet[] = { IC_UPLOAD_CHAR, charBufferId };

		// send command
		writePackage(PACKET_TYPE_COMMAND, packet);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}

		int idx = 0;
		int[] data;

		while (idx < model.length) {
			if (model.length - idx > dataPackageLength) {
				data = new int[dataPackageLength];
			} else {
				data = new int[model.length - idx];
			}
			for (int i = 0; i < data.length; i++) {
				data[i] = model[idx];
				idx++;
			}
			if (idx < model.length) {
				writePackage(PACKET_TYPE_DATA, data);
			} else {
				writePackage(PACKET_TYPE_ENDDATA, data);
			}
		}

		return model == downloadModel(charBufferId, defaultTimeout);
	}

	/**
	 * Loads fingerprint template from Flash library specified location into
	 * specified char buffer.
	 *
	 * @param fingerId
	 *            Flash location of the template.
	 * @param charBufferId
	 *            character file buffer number (1 or 2).
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void loadModel(int fingerId, int charBufferId, long timeout)
			throws SerialPortException, FingerprintSensorException {
		// create command data
		int commandData[] = { IC_LOAD_CHAR, charBufferId, fingerId / 256, fingerId % 256 };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Stores template from specified char buffer in Flash fingerprint library
	 * specified location.
	 *
	 * @param fingerId
	 *            Flash location of the template.
	 * @param charBufferId
	 *            character file buffer number (1 or 2).
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void storeModel(int fingerId, int charBufferId, long timeout)
			throws SerialPortException, FingerprintSensorException {
		// create command data
		int commandData[] = { IC_STORE, charBufferId, fingerId / 256, fingerId % 256 };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Generates template by combining charBuffer1 and charBuffer2, the result
	 * is stored back in both.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 */
	public void createModel(long timeout) throws SerialPortException, FingerprintSensorException {
		// create command data
		int commandData[] = { IC_CREATE_MODEL };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}
	}

	/**
	 * Uploads captured image to the host computer.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @return 2D array of integers representing pixels of the image.
	 */
	public int[][] getImageScan(long timeout) throws SerialPortException, IOException {
		// create command data
		int[] commandData = { IC_DOWNLOAD_IMAGE };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}

		int[][] imageBuffer = new int[288][256];
		int idx = 0;
		while (reply.type != PACKET_TYPE_ENDDATA) {
			reply = readPackage(timeout);

			for (int i = 0; i < reply.data.length; i++) {
				if (idx < 288 * 256) {
					// upper bits only
					imageBuffer[idx / 256][idx % 256] = (reply.data[i] / 16) * 16;
					idx++;
					imageBuffer[idx / 256][idx % 256] = (reply.data[i] % 16) * 16;
					idx++;
				}
			}
		}

		return imageBuffer;
	}

	/**
	 * Uploads captured image from the host computer to the image buffer.
	 *
	 * @param timeout
	 *            the timeout in milliseconds.
	 * @param scan
	 *            2D array of integers representing pixels of the image,
	 *            expected size 288 * 256.
	 */
	public void uploadImageScan(long timeout, int[][] scan) throws SerialPortException, IOException {
		if (scan.length != 288 || scan[0].length != 256) {
			throwFingerprintException(WRONG_SCAN_SIZE);
		}

		// create command data
		int[] commandData = { IC_UPLOAD_IMAGE };

		// send command
		writePackage(PACKET_TYPE_COMMAND, commandData);

		// receive command acknowledgement
		Package reply = readPackage(timeout);
		if ((reply == null) || (reply.type != PACKET_TYPE_ACK) || (reply.data.length != 1)) {
			throwFingerprintException(-1);
		} else if (reply.data[0] != CC_OK) {
			throwFingerprintException(reply.data[0]);
		}

		int idx = 0;
		int[] data = new int[dataPackageLength];

		while (idx < 288 * 256) {
			for (int i = 0; i < data.length; i++) {
				data[i] = (scan[idx / 256][idx % 256] / 16) * 16;
				idx++;
				data[i] += (scan[idx / 256][idx % 256] / 16);
				idx++;
			}
			if (idx < 288 * 256) {
				writePackage(PACKET_TYPE_DATA, data);
			} else {
				writePackage(PACKET_TYPE_ENDDATA, data);
			}
		}
	}

	/**
	 * Updates the package prolog with respect to current setting.
	 */
	private void updatePackageProlog() {
		// header
		packageProlog[0] = packageHeader / 256;
		packageProlog[1] = packageHeader % 256;

		// module address
		long address = moduleAddress;
		for (int i = 0; i < 4; i++) {
			packageProlog[5 - i] = (int) (address % 256);
			address = address / 256;
		}
	}

	/**
	 * Throws exception with the right message.
	 *
	 * @param confirmationCode
	 *            the confirmation code received in reply package.
	 */
	private void throwFingerprintException(int confirmationCode) throws FingerprintSensorException {
		String message;

		switch (confirmationCode) {
		case CC_OK:
			return;
		case CC_PACKET_RECEIVE_ERR:
			message = "Error when receiving data package.";
			break;
		case CC_IMAGE_FAIL:
			message = "Failed to enroll the finger.";
			break;
		case CC_IMAGE_MESSY:
			message = "Failed to generate character file due to the over-disorderly fingerprint image.";
			break;
		case CC_FEATURE_FAIL:
			message = "Failed to generate character file due to lackness of character points.";
			break;
		case CC_ENROLL_MISMATCH:
			message = "Failed to combine the character files.";
			break;
		case CC_BAD_LOCATION:
			message = "Addressing page ID is beyond the fingerprint library.";
			break;
		case CC_READ_TEMP_ERR:
			message = "Error when reading template from library or the template is invalid.";
			break;
		case CC_UPLOAD_TEMP_ERR:
			message = "Error when uploading template.";
			break;
		case CC_PACKET_RESPONSE_ERR:
			message = "Module can not receive the following data packages.";
			break;
		case CC_UPLOAD_IMAGE_ERR:
			message = "Error when uploading image.";
			break;
		case CC_DELETE_TEMP_FAIL:
			message = "Failed to delete the template.";
			break;
		case CC_EMPTY_LIB_FAIL:
			message = "Failed to clear finger library.";
			break;
		case CC_INCORRECT_PASSWORD:
			message = "Incorrect password.";
			break;
		case CC_INVALID_IMAGE:
			message = "Failed to generate the image for the lack of valid primary image.";
			break;
		case CC_FLASH_ERR:
			message = "Error when writing to flash.";
			break;
		case CC_INVALID_REGISTER:
			message = "Invalid register number.";
			break;
		case CC_WRONG_ADDRESS:
			message = "Wrong address code.";
			break;
		case CC_VERIFY_PASSWORD:
			message = "Must verify the password.";
			break;
		case WRONG_SCAN_SIZE:
			message = "Expected scan array 288x256.";
			break;
		default:
			message = "Unknown error.";
		}
		throw new FingerprintSensorException(message);
	}

	private void writePackage(int type, int[] data) throws SerialPortException {
		serialPort.writeIntArray(packageProlog);
		serialPort.writeInt(type);
		int length = data.length + 2;
		serialPort.writeInt(length / 256);
		serialPort.writeInt(length % 256);
		serialPort.writeIntArray(data);

		int checksum = type + (length / 256) + (length % 256);
		for (int dataByte : data) {
			checksum += dataByte;
		}

		serialPort.writeInt(checksum / 256);
		serialPort.writeInt(checksum % 256);
	}

	private Package readPackage(long timeout) throws SerialPortException {
		// convert timeout to nanoseconds
		timeout = timeout * 1_000_000;
		long startTime = System.nanoTime();

		// read package prolog
		int prologMatchLength = 0;
		while (prologMatchLength < packageProlog.length) {
			// check timeout
			long remainingTime = timeout - (System.nanoTime() - startTime);
			if (remainingTime < 0) {
				break;
			}

			// check received byte with the expected value
			if (packageProlog[prologMatchLength] == serialPortReader.readByte(remainingTime / 1_000_000)) {
				prologMatchLength++;
			} else {
				prologMatchLength = 0;
			}
		}

		// reading completed without matching package prolog
		if (prologMatchLength != packageProlog.length) {
			return null;
		}

		// read package type and its length
		int[] metadata = serialPortReader.readBytes(3, (timeout - (System.nanoTime() - startTime)) / 1_000_000);
		if (metadata == null) {
			return null;
		}

		int packageLength = metadata[1] * 256 + metadata[2];
		if (packageLength < 2) {
			return null;
		}

		// read package payload
		int[] data = serialPortReader.readBytes(packageLength - 2,
				(timeout - (System.nanoTime() - startTime)) / 1_000_000);

		// read checksum
		int[] checksumData = serialPortReader.readBytes(2, (timeout - (System.nanoTime() - startTime)) / 1_000_000);

		// compute checksum
		int checksum = 0;
		for (int dataByte : metadata) {
			checksum += dataByte;
		}
		for (int dataByte : data) {
			checksum += dataByte;
		}

		// verify checksum
		if ((checksumData[0] != checksum / 256) || (checksumData[1] != checksum % 256)) {
			return null;
		}

		return new Package(metadata[0], data);
	}
}
