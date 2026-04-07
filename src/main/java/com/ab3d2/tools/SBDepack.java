/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ab3d2.tools;

import java.io.*;
import java.nio.file.*;

public class SBDepack {

    static class BitReader {

        byte[] data;
        int pos;
        int bitBuf;
        int bitCount;

        BitReader(byte[] data, int offset) {
            this.data = data;
            this.pos = offset;
        }

        int readBit() {

            if (bitCount == 0) {
                bitBuf = data[pos++] & 0xFF;
                bitCount = 8;
            }

            int bit = bitBuf & 1;

            bitBuf >>= 1;
            bitCount--;

            return bit;
        }

        int readBits(int n) {

            int v = 0;

            for (int i = 0; i < n; i++) {
                v |= readBit() << i;
            }

            return v;
        }

        int readByte() {
            return readBits(8);
        }
    }

    static int readInt(byte[] b, int o) {

        return ((b[o] & 0xFF) << 24)
                | ((b[o + 1] & 0xFF) << 16)
                | ((b[o + 2] & 0xFF) << 8)
                | (b[o + 3] & 0xFF);
    }

    public static byte[] depack(byte[] file) {

        if (file[0] != '=' || file[1] != 'S' || file[2] != 'B' || file[3] != '=') {
            throw new RuntimeException("Invalid SB file");
        }

        int unpackSize = readInt(file, 4);

        byte[] out = new byte[unpackSize];

        BitReader br = new BitReader(file, 12);

        int dst = unpackSize;

        while (dst > 0) {

            int flag = br.readBit();

            if (flag == 1) {

                out[--dst] = (byte) br.readByte();

            } else {

                int offset = br.readBits(12);
                int length = br.readBits(4) + 3;

                for (int i = 0; i < length; i++) {

                    out[dst - 1] = out[dst + offset];
                    dst--;
                }
            }
        }

        return out;
    }

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {

            System.out.println("Usage: SBDepack input output");
            //return;
        }

        //String original_file = args[0];
        //String destination_file = args[1];
        String original_file = "C:\\Users\\guill\\Documents\\NetBeansProjects\\ab3d2-tkg-original\\media\\levels_editor_uncompressed\\LEVEL_A\\twolev.bin";
        String destination_file = "C:\\Users\\guill\\Documents\\NetBeansProjects\\ab3d2-tkg-original\\media\\levels_editor_uncompressed\\LEVEL_A\\twolev.bin.unpacked";

        byte[] file = Files.readAllBytes(Path.of(original_file));

        byte[] out = depack(file);

        Files.write(Path.of(destination_file), out);

        System.out.println("Decompressed : " + out.length);
    }
}
