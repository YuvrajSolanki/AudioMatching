package audiomatching;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioMatchingUtilities {

    // Stores the file data information in the frequency domain
    private ArrayList<double[]> freqData;
    // Stores the file data information in the time domain
    private ArrayList<Double> timeData;
    // Stores the audio fingerprints of each processed audio file
    private final HashMap<String, ArrayList<String>> hashData;
    // Stores the mapping of file names between actual files and coverted
    // files stored in the /tmp/tempSoundFiles/ folder
    private final HashMap<String, String> fileMapping;
    // The intensity to which the audio fingerprints are to be matched
    private final int INTENSITY;
    // The maximum difference that can be ignored while matching
    // the audio fingerprints
    private final int THRESHOLD;
    // The accuracy consideration of the matching function. Presently 70%
    private final double ACCURACY;
    // Size of each token while tokenizing the audio files
    private final int TOKEN_SIZE;
    // Max values for frequency
    private final int MAX;
    // The range of frequencies that are considered for fingerprinting
    private final int[] CONSIDERED_POINTS;

    // Initialize all the required variables and data structures at the
    // time of object creation
    AudioMatchingUtilities() {
        fileMapping = new HashMap();
        hashData = new HashMap();
        
        INTENSITY = 4;
        THRESHOLD = 5;
        ACCURACY = 0.7;
        TOKEN_SIZE = 1024;
        MAX = TOKEN_SIZE;
        
        CONSIDERED_POINTS = new int[4];
        CONSIDERED_POINTS[0] = 128;
        CONSIDERED_POINTS[1] = 256;
        CONSIDERED_POINTS[2] = 512;
        CONSIDERED_POINTS[3] = MAX + 1;
    }

    // Given: An audio file name.
    // Processing: Converts the given file to a base format using the 
    //             decode() method, reads it's data in time domain, converts
    //             the time domain data to corresponding frequency domain data
    //             and constructs it's fingerprint
    // Returns: returns false and prints an error message if file doesn't exist
    //          returns true if the audio file is properly initialized.
    public boolean initAudio(String fileName) {
        File fileTmp = new File(fileName);
	String original = fileName;
        // Initially convert every file to a base format
        decode(fileName);
        try {
            if (fileMapping.containsKey(fileTmp.getCanonicalPath())) {
                fileName = fileMapping.get(fileTmp.getCanonicalPath());
                File file = new File(fileName);

                // Check if the file exists
                if (file.exists()) {
                    // Check if the file is in WAVE format
                    if (isWave(fileName)) {
                        // Read the file to initialize time domain data
                        readAudio(fileName);
                        // Convert the time domain data to frequency domain data
                        convertToFrequency();
                        // Construct the file's audio fingerprint
                        getHash(fileName);
                        // return true if the file is processed properly
                        return true;
                    } else {
                        // Throw an error is the file is not in supported format
                        System.err.println("ERROR: "
                                + original + " is not a supported format");
			System.exit(-1);
                        return false;
                    }
                } else {
                    // Throw an error if the input file does not exist
                    System.err.println("ERROR: "
                            + original + " does not exist or is not supported");
                    // Exit with a non-zero status
                    System.exit(-1);
                    return false;
                }
            } else {
                // Throw an error if the input file is not in correct format
                System.err.println("ERROR: "
			    + original + " is not a supported format");
                System.exit(-1);
                return false;
            }
        } catch (IOException ex) {
        }
        return false;
    }

    // Given: A directory name
    // Returns: -- Returns false and prints an error message if directory
    //          doesn't exist
    //          -- Returns true if all the audio files in the directory are
    //          properly initialized
    // Process: Initializes every file present in the directory using 
    //          the initAudio() method
    public boolean initDirectory(String dirName) {
        File directory1 = new File(dirName);
        boolean flag;
        // Check if the input directory name is actually a directory or not
        if (directory1.isDirectory()) {
            File[] filesInDirectory1 = directory1.listFiles();
            for (File f : filesInDirectory1) {
                try {
                    if (f.isFile()) {
                        // Initialize every file in this directory
                        flag = initAudio(f.getCanonicalPath());

                        if (flag == false) {
                            return false;
                        }
                    }
                } catch (IOException ex) {
                }
            }
        } else {
            // Throw an error if the directory is not found
            System.err.println("ERROR: Directory "+ dirName + " not found");
            // Exit with a non-zero exit status
            System.exit(-1);
            return false;
        }
        return true;
    }

    // Given: An audio file's name
    // Process: Uses the lame script installed on the ccis machine to convert
    // the given file to a base format (WAVE format) and maps the original
    // file name with the new file's location (in /tmp/tempSoundFiles/)
    private void decode(String fileName) {
        File file = new File(fileName);
        try {
            String source = file.getCanonicalPath();
            String target = "/tmp/tempSoundFiles/" + file.getName();
            fileMapping.put(source, target);
            String command = "/course/cs5500f14/bin/lame --decode "
                    + source + " " + target;
            // Run the lame script to convert the source file 
            // to the target file and store it at the mentioned location
            Runtime.getRuntime().exec(command);
            // Put the current thread to sleep till the command completes
            // it's execution
            Thread.sleep(200);
        } catch (Exception e) {
        }
    }

    // Given: An audio file's name
    // Reads data from that audio file(time data) and stores it an Array List
    // for further processing
    private void readAudio(String fileName) {

        try {
            timeData = new ArrayList();
            File file = new File(fileName);
            AudioInputStream audioInputStream 
                    = AudioSystem.getAudioInputStream(file);
            int frameSize = audioInputStream.getFormat().getFrameSize();

            int numBytes = TOKEN_SIZE * frameSize;
            byte[] timeBytes = new byte[numBytes];
            try {
                // Read the audio file in small chunks
                while (audioInputStream.read(timeBytes) != -1) {
                    // Add the chunk to the ArrayList holding time data
                    for (Byte p : timeBytes) {
                        timeData.add(p.doubleValue());
                    }
                }
                audioInputStream.close();
            } catch (Exception ex) {
                System.err.println("ERROR: Reading file");
		System.exit(-1);
            }
        } catch (UnsupportedAudioFileException e) {
            System.err.println("ERROR: Reading file");
	    System.exit(-1);
        } catch (IOException e) {
            System.err.println("ERROR: Reading file");
	    System.exit(-1);
        }
    }

    // Divides time data into chunks of specific length and converts them into
    // frequency domain data using Fast Fourier Transformation. Frequency 
    // domain data is stored in the ArrayList freqData for further processing
    private void convertToFrequency() {
        FFT fft = new FFT(MAX);
        double[] timeBytes;
        timeBytes = toDoubleArray(timeData);
        freqData = new ArrayList();

        final int totalSize = timeBytes.length;
        
        int noOfTokens = totalSize / TOKEN_SIZE;
        // Run fast fourier transformation after forming chunks from the time
        // domain data
        for (int tokenNumber = 0; tokenNumber < noOfTokens; tokenNumber++) {
            double[] complex_re = new double[TOKEN_SIZE];
            double[] complex_im = new double[TOKEN_SIZE];

            // Put the real part as the time domain information and 
            // imaginary part as 0 and give this input to fft
            for (int i = 0; i < TOKEN_SIZE; i++) {
                complex_re[i] = timeBytes[(tokenNumber * TOKEN_SIZE) + i];
                complex_im[i] = 0;
            }
            // Run fast fourier transform on the given chunk to get the
            // corresponding frequency domain data
            fft.fft(complex_re, complex_im);
            // Take the real part of that data and add the chunk to the
            // ArrayList holding frequency domain data
            freqData.add(complex_re);
        }
    }

    // Given: an array list of Double data
    // Returns: a primitive array of double datatype after copying all data 
    //          from the given array list
    private double[] toDoubleArray(ArrayList<Double> timeData) {
        int size = timeData.size();
        double array[] = new double[size];
        int i = 0;
        for (Double p : timeData) {
            array[i++] = p;
        }
        return array;
    }

    // Given: An audio file's name
    // Generates hash code for all chunks by generating a string of the points
    // having highest magnitude between the considered points and taking a
    // hash code of that string
    // The hash code for each file is stored in a HashMap called hashData
    // The key for the HashMap is the file name and it's value is an ArrayList
    // of it's fingerpint data
    private void getHash(String fileName) {
        int[] consideredPoints = new int[CONSIDERED_POINTS.length];
        double[] maxScores = new double[CONSIDERED_POINTS.length];
        hashData.put(fileName, new ArrayList());

        for (int i = 0; i < CONSIDERED_POINTS.length; i++) {
            consideredPoints[i] = 0;
            maxScores[i] = 0;
        }

        // Take every slice of data from the frequency domain
        for (double[] slice : freqData) {
            for (int i = 0; i < MAX - 1; i++) {
                double mag = Math.log(slice[i] + 1);
                
                // Calculate the range number among the considered 
                // frequency ranges
                int pointNumber = getPointNumber(i);
                
                // Get the max values for every considered frequency range
                if (mag > maxScores[pointNumber]) {
                    maxScores[pointNumber] = mag;
                    consideredPoints[pointNumber] = i;
                }
            }
            String line = "";
            // Construct a string from all the considered points for the 
            // given chunk
            for (int i = 0; i < CONSIDERED_POINTS.length; i++) {
                line += consideredPoints[i] + "";
            }
            // Add the audio
            hashData.get(fileName).add(line.hashCode() + "");
        }
    }

    // Given: names of two audio files.
    // Performs matching on those files by matching the generated hash codes of 
    // all chunks and prints appropriate results
    // Process: Initially a base score is generated by matching the files with
    // themselves and the lowest score is taken as the base score. Then the
    // score is calculated by matching both files with each other and match
    // is judged by considering the scores and the accuracy value mentioned 
    // by the ACCURACY parameter
    // Note: The matchHelper() function is used to calculate the scores
    public void match(String key1, String key2) {
        String key1Old = key1;
        String key2Old = key2;
        if (fileMapping.containsKey(key1) && fileMapping.containsKey(key2)) {
            key1 = fileMapping.get(key1);
            key2 = fileMapping.get(key2);
            if (isLengthSame(key1, key2)) {
                // Get the highest score for file1
                double score1 = matchHelper(key1, key1, INTENSITY, THRESHOLD);
                // Get the highest score for file2
                double score2 = matchHelper(key2, key2, INTENSITY, THRESHOLD);
                double finalScore, percentScore, baseScore;

                // Initialize the base score as the minimum from the two
                // highest scores
                if (score1 <= score2) {
                    baseScore = score1;
                } else {
                    baseScore = score2;
                }

                // Match the files with each other and get the final score
                finalScore = matchHelper(key1, key2, INTENSITY, THRESHOLD);
                
                // Precent score lowers down the base score based on accuracy
                percentScore = baseScore * ACCURACY;

                // If the final score is greater or equal to the percent score
                // then print match
                if (finalScore >= percentScore) {
                    if (key1Old.contains("/")) {
                        key1Old = key1Old
                                .substring(key1Old.lastIndexOf("/") + 1);
                    }
                    if (key2Old.contains("/")) {
                        key2Old = key2Old
                                .substring(key2Old.lastIndexOf("/") + 1);
                    }
                    System.out.println("MATCH " + key1Old + " " + key2Old);
                }
            }
        }
    }

    // Given: names of two audio files along with the values of 
    //        intensity(power) and threshold(offset)
    // Generates a score which tells how many chunks are matching in those 
    // audio files. It is done by ignoring as many last digits as specified by
    // the power parameter and by keeping a particular threshold using the 
    // offset parameter
    private double matchHelper(String key1, String key2
            , int power, int offset) {
        // Get the audio fingerprint of file1
        ArrayList<String> hash1 = hashData.get(key1);
        // Get the audio fingerprint of file2
        ArrayList<String> hash2 = hashData.get(key2);
        double first;
        double second;
        double high, low;
        double score = 0;
        int size = 0;

        if (hash1.size() < hash2.size()) {
            size = hash1.size();
        } else {
            size = hash2.size();
        }

        // Match each fingerprint values of both the files with each other
        // using the power and offset parameters to compute a final match score
        for (int i = 0; i < size; i++) {
            first = Double.parseDouble(hash1.get(i));
            second = Double.parseDouble(hash2.get(i));

            first = (double) (first / Math.pow(10, power));
            second = (double) (second / Math.pow(10, power));

            if (first >= second) {
                high = first;
                low = second;
            } else {
                high = second;
                low = first;
            }

            if ((int) (high - low) <= offset) {
                score++;
            }
        }
        // return the computed score
        return score;
    }

    // Given: an index value of a chunk
    // Returns: the frequency range in which it belongs
    private int getPointNumber(int index) {
        int i = 0;
        while (CONSIDERED_POINTS[i] < index) {
            i++;
        }
        return i;
    }

    // Given: an audio file name
    // Returns: true if the file is a WAVE file else it will return false
    private boolean isWave(String fileName) {
        File inFile = new File(fileName);

        try {
            AudioFileFormat audioFileFormat 
                    = AudioSystem.getAudioFileFormat(inFile);
            return audioFileFormat.getType()
                    .toString().equalsIgnoreCase("WAVE");
        } catch (IOException ex) {
        } catch (UnsupportedAudioFileException ex) {
        }
        return false;
    }

    // Given: names of two audio files
    // Returns: true if they are of same length (in seconds) else false
    private boolean isLengthSame(String key1, String key2) {
        File file1 = new File(key1);
        File file2 = new File(key2);

        try {
            AudioInputStream inputStream 
                    = AudioSystem.getAudioInputStream(file1);
            AudioFormat format = inputStream.getFormat();
            long frames = inputStream.getFrameLength();
            double durationInSecondsFile1 
                    = (frames + 0.0) / format.getFrameRate();

            inputStream = AudioSystem.getAudioInputStream(file2);
            format = inputStream.getFormat();
            frames = inputStream.getFrameLength();
            double durationInSecondsFile2 
                    = (frames + 0.0) / format.getFrameRate();

            return (durationInSecondsFile1 == durationInSecondsFile2);

        } catch (UnsupportedAudioFileException ex) {
        } catch (IOException ex) {
        }
        return false;
    }
}
