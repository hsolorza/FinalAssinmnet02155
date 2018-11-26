/*
Author: Hannah Solorzano
        s181624
        CompArchHW3 RISC-V Simulator
This class provides the functionality for reading in a binary file and storing the data
into a string array.

 */
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Pattern;

public class ReadBinaryFile {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static int[] readInBinaryFile(String fileName) throws IOException {

        /*
        Read in file contents into byte array
         */
        Path path = Paths.get(fileName);
        System.out.println("File Name: " + path);
        byte[] binaryFileContents =  Files.readAllBytes(path);

        /*
        Reverse the file contents as required for Big Endian notation
         */
        for(int i = 0; i < binaryFileContents.length / 2; i++)
        {
            byte temp = binaryFileContents[i];
            binaryFileContents[i] = binaryFileContents[binaryFileContents.length - i - 1];
            binaryFileContents[binaryFileContents.length - i - 1] = temp;
        }

        /*
        Turn the byte array into hex string
         */
        char[] hexChars = new char[binaryFileContents.length * 2];
        for ( int j = 0; j < binaryFileContents.length; j++ ) {
            int v = binaryFileContents[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        String hexCharacters = new String(hexChars);
        String newHexString = hexCharacters.replaceAll("(.{8})(?!$)", "$1 ");

        Pattern pattern = Pattern.compile(" ");
        String [] words = pattern.split(newHexString);
        Arrays.toString(words);
        for(int i = 0; i < words.length/2; i++) {
            String temp = words[i];
            words[i] = words[words.length - i - 1];
            words[words.length - i - 1] = temp;
        }

        int[] result = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            result[i] = (int)Long.parseLong(words[i],16);
        }
        return result;

    }
}
