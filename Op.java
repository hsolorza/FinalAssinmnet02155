/*
Author: Hannah Solorzano
        s181624
        CompArchHW3 RISC-V Simulator
This class provides the functionality for parsing and performing the operations.

References:
https://riscv.org/specifications/
https://www.csl.cornell.edu/courses/ece4750/handouts/ece4750-tinyrv-isa.txt
https://rv8.io/isa.html
 */
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Op {

    static Boolean jumped;
    static int reg[] = new int[32];
    static int pc;
    static int memory[]= new int[4000000];

    public static void add(int[] progr) throws IOException {

        pc = 0;

        for (; ; ) {
            jumped = false;

            // For normal ops
            int instr = progr[pc / 4];
            int opcode = instr & 0x7f;
            int rd = (instr >> 7) & 0x01f;
            int funct3 = (instr >> 12) & 7;
            int res1 = (instr >> 15) & 0x01f;
            int res2 = (instr >> 20) & 0x01f;
            int funct7 = (instr >> 25) & 0x7f;
            int imm12 = (instr >> 20) & 0xFFF;
            int imm7 = (instr >> 25) & 0x7F;
            int imm5 = (instr >> 7) & 0x1f;
            int imm20 = (instr >> 12) & 0xFFFFF;
            // For JAL type ops
            int jalImm20 = (instr>>31) & 0x1;
            int jalImm19_12 = (instr>>12) & 0xFF;
            int jalImm_11 = (instr>>20) & 0x1;
            int jalImm1_10 = (instr>>21) & 0x3FF;
            int jalImm= (((jalImm20 << 19) + (jalImm19_12 << 11))
                        + (jalImm_11 << 10) + jalImm1_10) << 1;

            switch (opcode) {
                case 0x3:
                    op3(rd, res1, imm12, funct3);
                    break;
                case 0x37: //LUI
                    reg[rd] = imm20 << 12;
                    break;
                case 0x17: //AUIPC
                    reg[rd] = (imm20 << 12) + pc;
                    break;
                case 0x13:
                    op13(rd, res1, res2, imm12, funct3, funct7);
                    break;
                case 0x33:
                    op33(rd, res1, res2, imm12, funct3, funct7);
                    break;
                case 0x6F: //JAL
                    reg[rd] = pc + 4;
                    int signex = 0xFFFFF000 + jalImm;
                    boolean neg = jalImm20 == 1;
                    pc = neg ? signex + pc : jalImm + pc;
                    jumped = true;
                    break;
                case 0x67: //JALR
                    int immsignex = 0xFFFFF000 + jalImm;
                    reg[rd] = pc + 4;
                    boolean neg2 = imm12 >> 11 == 1;
                    pc = neg2 ? (reg[res1] + immsignex) & 0x7FFFFFFE : (reg[res1] + imm12) & 0x7FFFFFFE;
                    jumped = true;
                    break;
                case 0x23: // Save Byte ops
                    op23( imm5, imm7, imm12, res1, res2, funct3);
                    break;
                case 0x63: // Branch ops
                    op63(rd, res1, res2, imm7, imm5, imm12, pc, funct3, funct7);
                    break;
                case 0x73: //ECALL
                    op73();
                    break;
                default:
                    for (int i = 0; i < reg.length; ++i) {
                        System.out.print(reg[i] + " ");
                    }
                    //System.out.println("Opcode " + opcode + " not yet implemented");
                    break;
            }
            // check if a jump or branch op has been used
            if (!jumped) pc+=4;

            if (pc / 4 >= progr.length) {
                break;
            }
        }
        for (int i = 0; i < reg.length; ++i) {
            System.out.print(reg[i] + " ");
        }

        // Send register results to file
        byte[] bytes = new byte[reg.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            int b = 0;
            for (int j = 0; j < 8; j++)
                b = (b << 1) + reg[i * 8 + j];
            bytes[i] = (byte)b;
        }
        System.out.println();
        try (FileOutputStream fos = new FileOutputStream("myoutput.bin")) {
            fos.write(bytes);
        }

        System.out.println();
    }

    public static int[] readBinFile(String destination) throws IOException {
        ArrayList<Integer> progr=new ArrayList<>();
        DataInputStream binfile = new DataInputStream(new FileInputStream(destination));
        while (binfile.available() > 0) {
            try {
                int instruction = binfile.readInt();
                instruction = Integer.reverseBytes(instruction);
                progr.add(instruction);
            } catch (EOFException e) {
            }
        }
        binfile.close();

        int[] program = new int[progr.size()];
        Iterator<Integer> iterator = progr.iterator();
        for (int i = 0; i < program.length; i++) {
            program[i] = iterator.next().intValue();
        }
        return program;
    }

    public static void op3(int rd, int res1, int imm12, int op){
        int signex = imm12 + 0xFFFFF000;
        switch(op) {
            case 0x0: //LB
                if(imm12<0) {
                    reg[rd] = memory[reg[res1]+signex] < 0 ? ((memory[reg[res1]+signex])& 0xFF) + 0xFFFFFF00 :
                            ((memory[reg[res1]+imm12])& 0xFF);
                }
                else {
                    reg[rd] = memory[reg[res1]+imm12] < 0  ? ((memory[reg[res1]+imm12])& 0xFF) + 0xFFFFFF00 :
                                ((memory[reg[res1]+imm12])& 0xFF);
                }
                break;
            case 0x1: //LH
                if(imm12 < 0) {
                    reg[rd] = memory[reg[res1]+signex] < 0 ? ((memory[reg[res1]+signex])& 0xFFFF) + 0xFFFF0000 :
                            ((memory[reg[res1]+signex])& 0xFFFF);
                }
                else {
                    reg[rd] = memory[reg[res1]+imm12] < 0 ? ((memory[reg[res1]+imm12])& 0xFFFF) + 0xFFFF0000 :
                            ((memory[reg[res1]+imm12])& 0xFFFF);
                }
                break;
            case 0x2: //LW
                reg[rd] = imm12 < 0 ? memory[ reg[res1] + signex] : memory[ reg[res1] + imm12];
                break;

        }
    }

    public static void op13(int rd, int res1, int res2, int imm12, int op, int funct7) {
        boolean neg = (imm12 >> 11) == 1;
        int immsignex = 0xFFFFF000 + imm12;
        switch (op) {
            case 0x0: //ADDI
                reg[rd] = neg ? immsignex + (reg[res1]) : imm12 + (reg[res1]);
                break;
            case 0x2: // SLTI
                if (neg) {
                    reg[rd] = reg[res1] < immsignex ? 1 : 0;
                } else {
                    reg[rd] = reg[res1] < imm12 ? 1 : 0;
                }
                break;
            case 0x3: //SLTIU
                int compSigned = Long.compare(reg[res1] + Long.MIN_VALUE, immsignex + Long.MIN_VALUE);
                int compUnsigned = Long.compare(reg[res1] + Long.MIN_VALUE, imm12 + Long.MIN_VALUE);
                if (neg) {
                    reg[rd] = compSigned < 0 ? 1 : 0;
                } else {
                    reg[rd] = compUnsigned < 0 ? 1 : 0;
                }
                break;
            case 0x4: //XORI
                reg[rd] = neg ? (immsignex) ^ (reg[res1]) : (imm12) ^ (reg[res1]);
                break;
            case 0x6: //ORI
                reg[rd] = neg ? immsignex | (reg[res1]) : (imm12) | (reg[res1]);
                break;
            case 0x7: //ANDI
                reg[rd] = neg ? immsignex & reg[res1] : (imm12) & (reg[res1]);
                break;
            case 0x1: //SLLI
                reg[rd] = reg[res1] << res2;
                break;
            case 0x5:
                op13case5(rd, res1, res2, imm12, funct7);
                break;
        }
    }

    public static void op13case5(int rd, int res1, int res2, int imm12, int op) {
        switch (op) {
            case 0x0: //SRLI
                reg[rd] = reg[res1] >>> (res2 & 0x1F);
                break;
            case 0x20: //SRAI
                reg[rd] = reg[res1] >> (imm12 & 0x1F);
                break;
        }
    }

    public static void op33(int rd, int res1, int res2, int imm12, int op, int funct7){
        switch(op){
             case 0x0: // ADD, SUB
                op33case0(rd, res1, res2, imm12, funct7);
                break;
             case 0x1: //SLL
                reg[rd] = reg[res1] << (reg[res2] & 0x1F);
                break;
             case 0x2: //SLT
                reg[rd] = reg[res1] < reg[res2] ? 1 : 0;
                break;
             case 0x3: //SLTU
                int comp = Long.compare(reg[res1] + Long.MIN_VALUE, reg[res2] + Long.MIN_VALUE);
                reg[rd] = comp < 0 ? 1 : 0;
                break;
             case 0x4: //XOR
                reg[rd] = reg[res1] ^ reg[res2];
                break;
             case 0x5: // SRL, SRA
                op33case5(rd, res1, res2, imm12, funct7);
                break;
             case 0x6: //OR
                reg[rd] = reg[res1] | reg[res2];
                break;
             case 0x7://AND
                reg[rd] = reg[res1] & reg[res2];
                break;
        }
    }

    public static void op33case0(int rd, int res1, int res2, int imm12, int op){
        switch (op) {
            case 0x0: //add
                reg[rd] = reg[res1] + reg[res2];
                break;
            case 0x20: //sub
                reg[rd] = reg[res1] - reg[res2];
                break;
        }
    }

    public static void op33case5(int rd, int res1, int res2, int imm12, int op){
        switch (op) {
            case 0x0://SRL
                reg[rd] = reg[res1] >>> (reg[res2] & 0x1F);
                break;
            case 0x20://SRA
                reg[rd] = reg[res1] >> (reg[res2] & 0x1F);
                break;
        }
    }

    public static void op63(int rd, int res1, int res2, int imm7, int imm5, int imm12, int PC, int op, int funct7){
        boolean neg = imm12 >> 11 == 1;
        imm12 = (imm7 << 5) + imm5;
        int signex = 0xFFFFF000 + imm12;
        switch (op) {
            case 0x0: //BEQ
                if (reg[res1] == reg[res2]) {
                    imm12 = (imm7 << 5) + imm5;
                    pc = neg ? pc + signex : pc + imm12;
                    jumped = true;
                }
                break;
            case 0x1: //BNE
                if (reg[res1] != reg[res2]) {
                    imm12 = (imm7 << 5) + imm5;
                    pc = neg ? pc + signex : pc + imm12;
                    jumped = true;
                }
                break;
            case 0x4: //BLT
                if (reg[res1] < reg[res2]) {
                    pc = neg ?  pc + (signex) : pc + imm12;
                    jumped = true;
                }
                break;
            case 0x5: //BGE
                if (reg[res1] >= reg[res2]) {
                    pc =  neg ?  pc + signex :  pc + imm12;
                    jumped = true;
                }
                break;
            case 0x6: //BLTU
                int comp = Long.compare(reg[res1] + Long.MIN_VALUE, reg[res2] + Long.MIN_VALUE);
                if (comp < 0) {
                    pc = neg ? pc + signex : pc + imm12;
                    jumped = true;
                }
                break;
            case 0x7: //BGEU
                int comp2 = Long.compare(reg[res1] + Long.MIN_VALUE, reg[res2] + Long.MIN_VALUE);
                if (comp2 >= 0) {
                    pc = neg ? pc + signex : pc + imm12;
                    jumped = true;
                }
                break;
        }
    }

    public static void op73() {
        for (int i = 0; i < reg.length; ++i) {
            System.out.print(reg[i] + " ");
        }
        System.exit(0);
    }

    public static void op23(int imm5, int imm7, int imm12, int res1, int res2, int funct3){
        imm12=(imm7<<5)+imm5;
        boolean neg = imm12 >>> 11 == 1;
        int signedintimm = neg ? imm12 + 0xFFFFF000: imm12;
        switch(funct3) {
            case 0x0: //SB
                memory[(reg[res1] + signedintimm)]=(byte) (reg[res2] & 0xFF);
                break;

            case 0x1: //SH
                byte firstByte =  (byte)(reg[res2] & 0xFF);
                byte secondByte = (byte)((reg[res2] >> 8) & 0xFF);

                memory[reg[res1] + signedintimm] = firstByte;
                memory[reg[res1] + signedintimm + 1] = secondByte;
                break;

            case 0x2: //SW
                int signedint = neg ? imm12 + 0xFFFFF000: imm12;

                byte firstBit = (byte)((reg[res2] >> 8) & 0xFF);
                byte secondBit = (byte)((reg[res2] >> 16) & 0xFF);
                byte thirdBit = (byte)((reg[res2] >> 16) & 0xFF);
                byte fourthBit = (byte)((reg[res2] >> 24) & 0xFF);

                memory[reg[res1] + signedintimm] = firstBit;
                memory[reg[res1] + signedintimm + 1] = secondBit;
                memory[reg[res1] + signedintimm + 2] = thirdBit;
                memory[reg[res1] + signedintimm + 3] = fourthBit;
                break;

        }
    }
}