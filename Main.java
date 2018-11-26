/*
    Author: Hannah Solorzano
            s181624
            CompArchHW3 RISC-V Simulator

 */

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        int [] s= {0};
        try {
            s = ReadBinaryFile.readInBinaryFile("./src/shift.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Op.add(s);
    }
}