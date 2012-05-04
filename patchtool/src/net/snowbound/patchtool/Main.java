/*
 * patchtool - opensource tool to manipulate PTPatch files.
 * This is the first in a set of tools to aide modders in
 * creating PTPatch formatted binary patches. Used mainly for
 * the development and research of mods for Minecraft Pocket Edition
 *   
 *   */

package net.snowbound.patchtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class Main {
	public static void main(String[] args){
		if(args.length == 0){
			
			return;
		}
		if(args[0].equals("-cl")){//combine legacy
			int num_patches = (args.length - 1);
			String[] patches = new String[num_patches];
			for(int i = 0; i < num_patches; i++){
				patches[i] = args[i + 1];
			}
			
			try {
				combine_legacy(num_patches, patches);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static void combine_legacy(int num, String[] patches) throws IOException{
		byte[][] patch_array = new byte[num][];
		int final_size = 0;
		int header = (6 +(num * 4));
		for(int i = 0; i < num; i++){
			try {
				patch_array[i] = readPatch(i, patches);
				final_size += patch_array[i].length;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		final_size += header;
		File out = new File("patch.mod");
		out.delete();
		OutputStream os= new FileOutputStream(out);
		writeMagic(os);
		writeVersionCode(0, os);
		writeNumberPatches(num, os);
		os.write(generateIndices(num, patch_array));
		os.write(mergeAndStripHeaderData(num, patch_array));
		os.close();
	}
	
	public static void writeMagic(OutputStream os) throws IOException{
		byte[] magic =  {(byte) 0xFF, 0x50, 0x54,0x50 };
		os.write(magic);
	}
	
	public static void writeVersionCode(int vc, OutputStream os) throws IOException{
		os.write(vc);
	}
	
	public static void writeNumberPatches(int num, OutputStream os) throws IOException{
		os.write(num);
	}
	
	public static byte[] generateIndices(int num, byte[][] patchData){
		byte[] ret = new byte[num * 4];
		int headerSize = (6 + (num * 4));
		
		int bloat = 0;
		
		for(int i = 0; i < num; i++){
			int temp = headerSize;
			byte[] data = new byte[4];
			if(i == 0){
				bloat += patchData[i].length;
				data = intToByteArray(temp);
				ret[i * 4] = data[0];
				ret[(i * 4) + 1] = data[1];
				ret[(i * 4) + 2] = data[2];
				ret[(i * 4) + 3] = data[3];
			}else{
				temp += bloat;
				data = intToByteArray(temp);
				ret[i * 4] = data[0];
				ret[(i * 4) + 1] = data[1];
				ret[(i * 4) + 2] = data[2];
				ret[(i * 4) + 3] = data[3];
				bloat += patchData[i].length;
			}
		}
		
		return ret;
	}
	
	public static byte[] mergeAndStripHeaderData(int num, byte[][] patchData){
		int size = 0;
		for(int i = 0; i < num; i++){
			size += (patchData[i].length - 5);
		}
		
		byte[] ret = new byte[size];
		int count = 0;
		for(int i = 0, i2 = 0; i < num; i++){
			for(i2 = 0; i2 < (patchData[i].length - 5);i2++){
				ret[count] = patchData[i][i2 + 5];
				count++;
			}
		}
		
		return ret;
	}
	
	public static byte[] readPatch(int index, String[] patches) throws IOException{
		File patch = new File(patches[index]);
		byte[] ret = new byte[(int) patch.length()];
		InputStream is = new FileInputStream(patches[index]);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	
	
	public static final byte[] intToByteArray(int value) {
        return new byte[] {
                (byte)(value >>> 24),
                (byte)(value >>> 16),
                (byte)(value >>> 8),
                (byte)value};
	}
}
