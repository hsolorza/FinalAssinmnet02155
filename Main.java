/*
    Author: Hannah Solorzano
            s181624
            DTU 02155 Copmuter Architecture
            Final Project RISC-V Simulator

 */

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int[] p;
        String destination = "./src/t1.bin";
        p = Op.readBinFile(destination);
        Op.add(p);
    }
}