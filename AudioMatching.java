package audiomatching;

import java.io.File;
import java.io.IOException;

public class AudioMatching {
    
    public static void main(String[] args) {
        try {
            if (args.length == 4) {
                
                // Makes a folder inside the /tmp/ folder to store the
                // converted audio files
                Runtime.getRuntime().exec("mkdir /tmp/tempSoundFiles");
                
                String param1 = args[0]; // Must be -f or -d
                String param2 = args[1]; // Must be a .mp3 or a .wav file
                String param3 = args[2]; // Must be -f or -d
                String param4 = args[3]; // Must be a .mp3 or a .wav file

                AudioMatchingUtilities utils = new AudioMatchingUtilities();

                // if the first parameter is a directory 
                // and the other parameter is a file
                if (param1.equals("-d") && param3.equals("-f")) {
                    if (utils.initDirectory(param2)) {
                        File file1 = new File(param4);
                        utils.initAudio(file1.getCanonicalPath());
                        
                        File directory1 = new File(param2);
                        File[] filesInDirectory1 = directory1.listFiles();
                        
                        // Match every file in the directory 
                        // with the given file
                        for (File f : filesInDirectory1) {
                            try {
                                utils.match(f.getCanonicalPath()
                                        , file1.getCanonicalPath());
                            } catch (IOException ex) {
                            }
                        }
                    }
                } 
                // if the first parameter is a directory 
                // and the other parameter is also a directory
                else if (param1.equals("-d") && param3.equals("-d")) {
                    if (utils.initDirectory(param2)
                            && utils.initDirectory(param4)) {
                        File directory1 = new File(param2);
                        File directory2 = new File(param4);
                        File[] filesInDirectory1 = directory1.listFiles();
                        File[] filesInDirectory2 = directory2.listFiles();
                        
                        // Match every file in the first directory
                        // with every file in the second directory
                        for (File f1 : filesInDirectory1) {
                            for (File f2 : filesInDirectory2) {
                                try {
                                    utils.match(f1.getCanonicalPath()
                                            , f2.getCanonicalPath());
                                } catch (IOException ex) {
                                }
                            }
                        }
                    }
                } 
                // if the first parameter is a file 
                // and the other parameter is also a file
                else if (param1.equals("-f") && param3.equals("-f")) {
                    File file1 = new File(param2);
                    File file2 = new File(param4);
                    
                    utils.initAudio(file1.getCanonicalPath());
                    utils.initAudio(file2.getCanonicalPath());
                    // Match both the files
                    utils.match(file1.getCanonicalPath()
                            , file2.getCanonicalPath());
                }
                // if the first parameter is a file 
                // and the other parameter is a directory
                else if (param1.equals("-f") && param3.equals("-d")) {
                    if (utils.initDirectory(param4)) {
                        File file1 = new File(param2);
                        utils.initAudio(file1.getCanonicalPath());
                        
                        File directory1 = new File(param4);
                        File[] filesInDirectory1 = directory1.listFiles();
                        
                        // Match every file in the directory 
                        // with the given file
                        for (File f : filesInDirectory1) {
                            try {
                                utils.match(f.getCanonicalPath()
                                        , file1.getCanonicalPath());
                            } catch (IOException ex) {
                            }
                        }
                    }
                } else {
		    System.err.println("ERROR: Incorrect arguments");
		    System.exit(-1);
		}
            } 
            // In case of erroneous parameters 
            else {
		System.err.println("ERROR: Wrong number of arguments");
            	System.exit(-1);
	    }
        } catch (Exception e) {
        } finally {
            try {
                // Delete the temp files at the end
                Runtime.getRuntime().exec("rm -fr /tmp/tempSoundFiles");
            } catch (IOException ex) {
            }
        }
    }
}
