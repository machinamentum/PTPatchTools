package com.joshuahuelsman.patchtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Main {
	//Default IP patch by Intyre of the Minecraft Forums (slightly modified)
	//Deprecated. Do not use for future use.
	public static byte[] defaultip = {
		(byte) 0xFF, 0x50, 0x54, 0x50, (byte) 0xBF, 0x01, 0x00,
		0x00, 0x00, 0x0A, 0x00, 0x17, (byte) 0xBB, 0x24, 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	};
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("PTPatchTool: a tool to manipulate the PTPatch format");
			System.out.println("Visit http://www.minecraftforum.net/topic/1112628-mod-patching/ for usage.");
			return;
		}
		if (args[0].equals("-cl")) {// combine legacy
			int num_patches = (args.length - 1);
			String[] patches = new String[num_patches];
			for (int i = 0; i < num_patches; i++) {
				patches[i] = args[i + 1];
			}

			try {
				combine_legacy(num_patches, patches);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(args[0].equals("-s")){
			sendViaADB(args[1]);
		}else if(args[0].equals("-ip")){
			try {
				generateIPPatch(args[1]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(args[0].equals("diff")){
			try {
				diff(args[1], args[2], args.length <= 3? null: args[3].getBytes(Charset.forName("UTF-8")));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else if (args[0].equals("patch")) {
			try {
				applyPatchTo(args[1], args[2]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("Incorrect command line.");
		}
	}

	public static void combine_legacy(int num, String[] patches)
			throws IOException {
		byte[][] patch_array = new byte[num][];
		// int final_size = 0;
		// int header = (6 +(num * 4));
		for (int i = 0; i < num; i++) {
			try {
				patch_array[i] = readPatch(i, patches);
				// final_size += patch_array[i].length;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// final_size += header;
		File out = new File("patch.mod");
		out.delete();
		OutputStream os = new FileOutputStream(out);
		writeMagic(os);
		writeVersionCode(0, os);
		writeNumberPatches(num, os);
		os.write(generateIndices(num, patch_array));
		os.write(mergeAndStripHeaderData(num, patch_array));
		os.close();
	}

	public static void writeMagic(OutputStream os) throws IOException {
		byte[] magic = { (byte) 0xFF, 0x50, 0x54, 0x50 };
		os.write(magic);
	}

	public static void writeVersionCode(int vc, OutputStream os)
			throws IOException {
		os.write(vc);
	}

	public static void writeNumberPatches(int num, OutputStream os)
			throws IOException {
		os.write(num);
	}

	public static byte[] generateIndices(int num, byte[][] patchData) {
		byte[] ret = new byte[num * 4];
		int headerSize = (6 + (num * 4));

		int bloat = 0;

		for (int i = 0; i < num; i++) {
			int temp = headerSize;
			byte[] data = new byte[4];
			if (i == 0) {
				bloat += patchData[i].length - 5;
				data = intToByteArray(temp);
				ret[i * 4] = data[0];
				ret[(i * 4) + 1] = data[1];
				ret[(i * 4) + 2] = data[2];
				ret[(i * 4) + 3] = data[3];
			} else {
				temp += bloat;
				data = intToByteArray(temp);
				ret[i * 4] = data[0];
				ret[(i * 4) + 1] = data[1];
				ret[(i * 4) + 2] = data[2];
				ret[(i * 4) + 3] = data[3];
				bloat += patchData[i].length - 5;
			}
		}
		
		
		
		return ret;
	}
	
	public static byte[] generateIndices(byte[][] patchData, int padding) {
		byte[] ret = new byte[patchData.length * 4];
		int headerSize = (6 + (patchData.length * 4)) + padding;

		int bloat = 0;

		for (int i = 0; i < patchData.length; i++) {
			int temp = headerSize;
			byte[] data = new byte[4];
			if (i == 0) {
				bloat += patchData[i].length;
				data = intToByteArray(temp);
				ret[i * 4] = data[0];
				ret[(i * 4) + 1] = data[1];
				ret[(i * 4) + 2] = data[2];
				ret[(i * 4) + 3] = data[3];
			} else {
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

	public static byte[] mergeAndStripHeaderData(int num, byte[][] patchData) {
		int size = 0;
		for (int i = 0; i < num; i++) {
			size += (patchData[i].length - 5);
		}

		byte[] ret = new byte[size];
		int count = 0;
		for (int i = 0, i2 = 0; i < num; i++) {
			for (i2 = 0; i2 < (patchData[i].length - 5); i2++) {
				ret[count] = patchData[i][i2 + 5];
				count++;
			}
		}

		return ret;
	}

	public static byte[] readPatch(int index, String[] patches)
			throws IOException {
		File patch = new File(patches[index]);
		byte[] ret = new byte[(int) patch.length()];
		InputStream is = new FileInputStream(patches[index]);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	
	public static byte[] readPatch(String patch)
			throws IOException {
		File patchf = new File(patch);
		byte[] ret = new byte[(int) patchf.length()];
		InputStream is = new FileInputStream(patch);
		is.read(ret, 0, ret.length);
		is.close();
		return ret;
	}
	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	public static void sendViaADB(String patch) {
		try {
			String line;
			Process p = Runtime.getRuntime().exec("adb push " + patch + " /mnt/sdcard");
			BufferedReader bri = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader bre = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			while ((line = bri.readLine()) != null) {
				System.out.println(line);
			}
			bri.close();
			while ((line = bre.readLine()) != null) {
				System.out.println(line);
			}
			bre.close();
			p.waitFor();
			System.out.println("Done.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void generateIPPatch(String ip) throws IOException{
		OutputStream out = new FileOutputStream(new File("patch.mod"));
		ByteBuffer buf = ByteBuffer.wrap(defaultip);
		buf.position(14);
		buf.put(ip.getBytes());
		out.write(buf.array());
		out.close();
	}

	public static void applyPatchTo(String fileLoc, String patchLoc) throws IOException {
		PTPatch patch = new PTPatch(patchLoc);
		patch.loadPatch();
		System.out.println(patchLoc + ": Minecraft version: " + Integer.toString(patch.mHeader.minecraft_ver, 16));
		System.out.println("Number of locations in patch: " + patch.mHeader.num_patches);
		if (!patch.checkMagic()) {
			System.err.println("Magic error!");
			return;
		}
		System.out.println("Magic OK.");
		File file = new File(fileLoc);
		patch.applyPatch(file);
		System.out.println("Done patching.");
	}

	public static void diff(String oldf, String newf, byte[] metaData) throws IOException{
		byte[] oldData = readPatch(oldf);
		byte[] newData = readPatch(newf);
		
		if(oldData.length != newData.length){
			System.out.println("Error: The new file's length does not match the old file's length. Aborting...");
			return;
		}
		File out = new File("patch.mod");
		out.delete();
		OutputStream os = new FileOutputStream(out);
		writeMagic(os);
		writeVersionCode(0, os);
		
		byte[][] patchData;
		
		int numPatches = 0;
		for(int i = 0; i < oldData.length; i++){
			if(oldData[i] != newData[i]){
				numPatches++;
				while(oldData[i] != newData[i]){
					i++;
				}
				
			}
		}
		System.out.println("Number of Patches: " + numPatches);
		writeNumberPatches(numPatches, os);
		patchData = new byte[numPatches][];
		int index = 0;
		int[] address = new int[numPatches];
		for(int i2 = 0; i2 < numPatches; i2++){
			for(int i = index; i < oldData.length; i++){
				if(oldData[i] != newData[i]){
					address[i2] = i;
					int i3 = 0;
					while(oldData[i] != newData[i]){
						i++;
						i3++;
					}
					patchData[i2] = new byte[i3 + 4];
					System.out.println("Length of patch " + i2 + ": " + (i3 + 4));
					index = i;
					
					break;
				}
			}
		}
		
		index = 0;
		for(int i2 = 0; i2 < numPatches; i2++){
			
			for(int i = index; i < oldData.length; i++){
				if(oldData[i] != newData[i]){
					
					byte[] addr = intToByteArray(i);
					patchData[i2][0] = addr[0];
					patchData[i2][1] = addr[1];
					patchData[i2][2] = addr[2];
					patchData[i2][3] = addr[3];
					while(oldData[i] != newData[i]){
						i++;
					}
					index = i;
					break;
				}
			}
		}
		
		for(int i2 = 0; i2 < numPatches; i2++){
			for(int i = 0; i < (patchData[i2].length - 4); i++){
				patchData[i2][i + 4] = newData[address[i2] + i];
			}
		}

		os.write((generateIndices(patchData, (metaData != null? metaData.length : 0))));
		if (metaData != null) os.write(metaData);

		for(int i = 0; i < numPatches; i++){
			os.write(patchData[i]);
		}
		os.close();
		
	}

}
