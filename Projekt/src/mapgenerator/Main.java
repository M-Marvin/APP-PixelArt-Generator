package mapgenerator;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import minecraftnbt.CompressedStreamTools;
import minecraftnbt.NBTTagCompound;
import minecraftnbt.NBTTagList;

public class Main {

	public static File imageFile;
	public static HashMap<String, Byte> ids = new HashMap<>();
	public static HashMap<String, Integer> blocks = new HashMap<>();
	public static HashMap<String, Integer> blockPriority = new HashMap<>();
	public static HashMap<String, String> blockStateNames = new HashMap<>();
	public static int[][] pixelArray;
	public static List<String> daniedBlocks;
	public static HashMap<String, Integer> usedBlocks;
	public static String[][] blockOrder;
	
	public static Frame frame;
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) {

		registerBlocks();

		daniedBlocks = new ArrayList<String>();
		usedBlocks = new HashMap<String, Integer>();
		
		frame = new Frame();
		frame.show();
		
	}
	
	public static void loadImage(File file) throws IOException {
		
		System.out.println("Load Image " + file);
		
		//MC Map Size
		int width = 128;
		int height = 128;
		
		BufferedImage image = ImageIO.read(file);
		int imageWidth = image.getWidth();
		int imageHeight = image.getHeight();
		
		int pixelZoomX = 1;
		int pixelZoomY = 1;
		
		if (imageWidth < width) pixelZoomX = (int) Math.ceil(width / imageWidth);
		if (imageHeight < height) pixelZoomY = (int) Math.ceil(height / imageHeight);
		
		imageWidth = width / pixelZoomX;
		imageHeight = height / pixelZoomY;
		
		int[] pixels = image.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);
		int[][] pixelArr = new int[width][height];
		
		int pixelCount = 0;
		for (int row = 0; row < height;) {
			
			int[] nRow = new int[width];
			
			for (int col = 0; col < width;) {
				
				for (int dx = 1; dx <= pixelZoomX; dx++) {
					
					nRow[col] = pixels[pixelCount];
					col++;
					
				}
				
				pixelCount++;
				
			}
			
			for (int dy = 1; dy <= pixelZoomY; dy++) {
				
				pixelArr[row] = nRow;
				row++;
				
			}
			
		}
		
		System.out.println("Converted Image Size: " + pixelArr[0].length + " " + pixelArr.length);
		
		pixelArray = pixelArr;
		
	}
	
	
	public static void generateMap() {
		
		blockOrder = new String[pixelArray[0].length][pixelArray.length];
		
		for (int y = 0; y < pixelArray.length; y++) {
			
			blockOrder[y] = new String[pixelArray[0].length];
			
			for (int x = 0; x < pixelArray[0].length; x++) {
				
				Color color = new Color(pixelArray[y][x]);
				String block = getBlockForColor(color);				

				int amount = usedBlocks.containsKey(block) ? usedBlocks.get(block) : 0;
				usedBlocks.remove(block);
				usedBlocks.put(block, 2 + amount);
				
				blockOrder[y][x] = block;
				
				
			}
			
		}
		
	}

	public static void saveAsMapFile(File file, boolean useOptimalColors) throws IOException {

		NBTTagCompound fileTag = new NBTTagCompound();
		NBTTagCompound dataTag = new NBTTagCompound();

		dataTag.setTag("banners", new NBTTagList());
		dataTag.setTag("frames", new NBTTagList());
		dataTag.setByte("dimension", (byte) 0);
		dataTag.setByte("locked", (byte) 1);
		dataTag.setByte("scale", (byte) 0);
		dataTag.setByte("trackingPosition", (byte) 0);
		dataTag.setByte("unlimitedTracking", (byte) 0);
		dataTag.setInteger("xCenter", 2147483647);
		dataTag.setInteger("yCenter", 2147483647);
		
		byte[] colors = new byte[blockOrder.length * blockOrder[0].length];
		
		if (useOptimalColors) {
			
			int[] colorP = new int[] {180, 220, 255, 135};
			
			int c = 0;
			for (int y = 0; y < pixelArray.length; y++) {
				
				for (int x = 0; x < pixelArray[0].length; x++) {
					
					Color color = new Color(pixelArray[y][x]);
					Color c1 = new Color(getColorForBlock(blockOrder[y][x]));
					float div = 255;
					int optimalColorP = 1;
					
					for (int i = 0; i < 4; i++) {
						
						int r = c1.getRed() * colorP[i] / 255;
						int g = c1.getGreen() * colorP[i] / 255;
						int b = c1.getBlue() * colorP[i] / 255;
						Color blockColor = new Color(r, g, b);
						
						float divRed = blockColor.getRed() > color.getRed() ? blockColor.getRed() - color.getRed() : color.getRed() - blockColor.getRed();
						float divGreen = blockColor.getGreen() > color.getGreen() ? blockColor.getGreen() - color.getGreen() : color.getGreen() - blockColor.getGreen();
						float divBlue = blockColor.getBlue() > color.getBlue() ? blockColor.getBlue() - color.getBlue() : color.getBlue() - blockColor.getBlue();
						float divGes = (divRed + divGreen + divBlue) / 3;
						
						if (divGes < div) {
							
							div = divGes;
							optimalColorP = i;
							
						}
						
					}
					
					byte id = getIdFromBlock(blockOrder[y][x], optimalColorP);
					colors[c++] = id;
					
				}
				
			}
			
		} else {
			
			int c = 0;
			for (String[] row : blockOrder) {
				
				for (String block : row) {
					
					colors[c++] = getIdFromBlock(block, 1);
					
				}
				
			}
			
		}
		
		dataTag.setByteArray("colors", colors);
		
		fileTag.setTag("data", dataTag);
		fileTag.setInteger("DataVersion", 2230);
		
		OutputStream os = new FileOutputStream(file);
		CompressedStreamTools.writeCompressed(fileTag, os);
		os.close();
		
	}
	
	public static void saveMapImage(File file) throws IOException {
		
		int width = blockOrder.length;
		int height = blockOrder[0].length;
		int[] pixels = new int[width * height];
		
		int c = 0;
		for (String[] row : blockOrder) {
			
			for (String block : row) {
				
				pixels[c++] = getColorForBlock(block);
				
			}
			
		}
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		ImageIO.write(image, "png", file);
		
	}
	
	public static void saveAsSchematic(File file) throws IOException {
		
		NBTTagCompound fileTag = new NBTTagCompound();
		NBTTagCompound paletteTag = new NBTTagCompound();
		
		List<Byte> blockDataBytes = new ArrayList<>();
		int palleteIdCount = 0;
		
		for (String[] row : blockOrder) {
			
			for (String block : row) {
				
				String blockState = getBlockStateStringForBlockIDName(block);
				if (!paletteTag.hasKey(blockState)) paletteTag.setInteger(blockState, palleteIdCount++);
				blockDataBytes.add((byte) paletteTag.getInteger(blockState));
				
			}
			
		}
		
		byte[] byteArr = new byte[blockDataBytes.size()];
		for (int i = 0; i < blockDataBytes.size(); i++) byteArr[i] = blockDataBytes.get(i);
		fileTag.setByteArray("BlockData", byteArr);
		
		fileTag.setTag("Palette", paletteTag);
		
		fileTag.setShort("Length", (short) (blockOrder.length));
		fileTag.setShort("Width", (short) (blockOrder[0].length));
		fileTag.setInteger("DataVersion", 1);
		
		OutputStream os = new FileOutputStream(file);
		CompressedStreamTools.writeCompressed(fileTag, os);
		os.close();
		
	}
	
	public static String getBlockStateStringForBlockIDName(String blockIDname) {
		
		String blockState = blockStateNames.get(blockIDname);
		
		if (blockState != null ? blockState.equals("") : true) {
			
			blockState = blockIDname;
			
		}
		
		return "minecraft:" + blockState;
		
	}
	
	public static int getColorForBlock(String block) {
		
		return blocks.get(block) != null ? blocks.get(block) : new Color(255, 255, 255).getRGB();
		
	}

	public static byte getIdFromBlock(String block, int colorP) {
		return (byte) (ids.get(block) * 4 + colorP);
	}
	
	public static String getBlockForColor(Color color) {
		
		String foundBlock = "";
		float div = 255;
		float pri = 255;
		
		for (Entry<String, Integer> blockEntry : blocks.entrySet()) {
			
			Color blockColor = new Color(blockEntry.getValue());
			
			float divRed = blockColor.getRed() > color.getRed() ? blockColor.getRed() - color.getRed() : color.getRed() - blockColor.getRed();
			float divGreen = blockColor.getGreen() > color.getGreen() ? blockColor.getGreen() - color.getGreen() : color.getGreen() - blockColor.getGreen();
			float divBlue = blockColor.getBlue() > color.getBlue() ? blockColor.getBlue() - color.getBlue() : color.getBlue() - blockColor.getBlue();
			float divGes = (divRed + divGreen + divBlue) / 3;
			
			if (divGes <= div && !daniedBlocks.contains(blockEntry.getKey())) {
				
				div = divGes;
				float priority = blockPriority.get(blockEntry.getKey());
				
				if (priority <= pri) {
					
					pri = priority;
					foundBlock = blockEntry.getKey();
					
				}
				
			}
			
		}
		
		return foundBlock;
		
	}
	
	public static void registerBlocks() {
		
		addBlock((byte) 1, 127, 178, 56, "grass");
		addBlock((byte) 2, 247, 233, 163, "1:sand", "2:birch_wood", "3:scaffolding", "4:glowstone", "5:end_stone", "6:bone_block");
		addBlock((byte) 3, 199, 199, 199, "mushroom_stem");
		addBlock((byte) 4, 255, 0, 0, "4:lava", "3:tnt", "2:fire", "1:redstone_block");
		addBlock((byte) 5, 160, 160, 255, "ice");
		addBlock((byte) 6, 167, 167, 167, "2:grindstone", "1:brewing_stand");
		addBlock((byte) 7, 0, 124, 0, "leaves");
		addBlock((byte) 8, 255, 255, 255, "2:snow_block", "4:white_wool", "3:white_glazed_terracotta", "1:white_concrete");
		addBlock((byte) 9, 164, 168, 184, "clay");
		addBlock((byte) 10, 151, 109, 77, "1:dirt", "2:granite", "3:jungle_wood", "4:brown_mushroom_block");
		addBlock((byte) 11, 112, 112, 112, "1:stone", "2:andesite", "3:gravel", "4:acacia_bark");
		addBlock((byte) 12, 64, 64, 255, "water");
		addBlock((byte) 13, 143, 119, 72, "oak_wood");
		addBlock((byte) 14, 255, 252, 245, "1:diorite", "2:birch_bark", "3:sea_lantern");
		addBlock((byte) 15, 216, 127, 51, "4:acacia_wood", "3:red_sand", "2:terracotta", "5:orange_wool", "6:orange_glazed_terracotta", "1:orange_concrete", "7:pumpkin", "2:orange_terracotta");
		addBlock((byte) 16, 178, 76, 215, "3:magenta_wool", "4:magenta_glazed_terracotta", "1:magenta_concrete", "2:purpur");
		addBlock((byte) 17, 102, 153, 216, "3:light_blue_wool", "2:light_blue_glazed_terracotta", "1:light_blue_concrete");
		addBlock((byte) 18, 229, 229, 51, "4:yellow_wool", "3:yellow_glazed_terracotta", "1:yellow_concrete", "2:hay_bale");
		addBlock((byte) 19, 127, 204, 25, "4:lime_wool", "1:lime_concrete", "3:lime_glazed_terracotta", "2:melon");
		addBlock((byte) 20, 242, 127, 165, "2:pink_wool", "1:pink_concrete");
		addBlock((byte) 21, 76, 76, 76, "1:gray_concrete", "2:gray_glazed_terracotta", "3:gray_wool");
		addBlock((byte) 22, 153, 153, 153, "3:light_gray_wool", "1:light_gray_concrete", "2:light_gray_glazed_terracotta");
		addBlock((byte) 23, 76, 127, 153, "3:cyan_wool", "1:cyan_concrete", "2:cyan_glazed_terracotta", "3:prismarine");
		addBlock((byte) 24, 127, 63, 178, "4:purple_wool", "1:purple_concrete", "2:purple_glazed_terracotta", "3:mycelium");
		addBlock((byte) 25, 51, 76, 178, "3:blue_wool", "1:blue_concrete", "2:blue_glazed_terracotta");
		addBlock((byte) 26, 102, 76, 51, "3:dark_oak_wood", "4:spruce_bark", "1:brown_concrete", "5:brown_wool", "2:brown_glazed_terracotta", "6:soul_sand");
		addBlock((byte) 27, 102, 127, 51, "4:green_wool", "1:green_concrete", "3:green_glazed_terracotta", "2:dried_kelp_block");
		addBlock((byte) 28, 153, 51, 51, "3:red_wool", "1:red_concrete", "2:red_glazed_terracotta", "5:bricks", "6:red_mushroom_block", "4:nether_wart_block");
		addBlock((byte) 29, 25, 25, 25, "4:black_wool", "1:black_concrete", "2:black_glazed_terracotta", "5:obsidian", "3:coal_block");
		addBlock((byte) 30, 250, 238, 77, "1:gold_block", "2:bell");
		addBlock((byte) 31, 92, 219, 213, "1:prismarine_bricks", "2:dark_prismarine");
		addBlock((byte) 32, 64, 128, 255, "lapis_block");
		addBlock((byte) 33, 0, 217, 58, "emerald_block");
		addBlock((byte) 34, 129, 86, 49, "1:podzol", "2:spruce_wood", "3:oak_bark", "4:jungle_bark");
		addBlock((byte) 35, 112, 2, 0, "1:netherrack", "3:nether_bricks", "5:nether_quartz_ore", "2:magma_block", "4:red_nether_bricks");
		addBlock((byte) 36, 209, 177, 161, "white_terracotta");
		addBlock((byte) 37, 159, 82, 36, "orange_terracotta");
		addBlock((byte) 38, 149, 87, 108, "magenta_terracotta");
		addBlock((byte) 39, 112, 108, 138, "light_blue_terracotta");
		addBlock((byte) 40, 186, 133, 36, "yellow_terracotta");
		addBlock((byte) 41, 103, 117, 53, "lime_terracotta");
		addBlock((byte) 42, 160, 77, 78, "pink_terracotta");
		addBlock((byte) 43, 57, 41, 35, "gray_terracotta");
		addBlock((byte) 44, 135, 107, 98, "light_gray_terracotta");
		addBlock((byte) 45, 87, 92, 92, "cyan_terracotta");
		addBlock((byte) 46, 122, 73, 88, "purple_terracotta");
		addBlock((byte) 47, 76, 62, 92, "blue_terracotta");
		addBlock((byte) 48, 76, 50, 35, "brown_terracotta");
		addBlock((byte) 49, 76, 82, 42, "green_terracotta");
		addBlock((byte) 50, 142, 60, 46, "red_terracotta");
		addBlock((byte) 51, 37, 22, 16, "black_terracotta");
		
		addBlockState("birch_wood", "birch_planks");
		addBlockState("spruce_wood", "spruce_planks");
		addBlockState("dark_oak_wood", "dark_oak_planks");
		addBlockState("oak_wood", "oak_planks");
		addBlockState("jungle_wood", "jungle_planks");
		addBlockState("acacia_wood", "acacia_planks");
		addBlockState("birch_bark", "birch_log[axis=x]");
		addBlockState("spruce_bark", "spruce_log[axis=x]");
		addBlockState("dark_oak_bark", "dark_oak_log[axis=x]");
		addBlockState("oak_bark", "oak_log[axis=x]");
		addBlockState("jungle_bark", "jungle_log[axis=x]");
		addBlockState("acacia_bark", "acacia_log[axis=x]");
		addBlockState("purpur", "purpur_block");
		addBlockState("water", "water[level=9]");
		addBlockState("bell", "bell[facing=north,attachment=floor,powered=false]");
		addBlockState("grindstone", "grindstone[face=floor,facing=north]");
		addBlockState("grass", "grass_block");
		addBlockState("ice", "packed_ice");
		
	}
	public static void addBlock(byte id, int r, int g, int b, String... blocknames) {
		
		if (blocknames.length > 1) {
			
			for (String block : blocknames) {
				
				ids.put(block.split("\\:")[1], id);
				blocks.put(block.split("\\:")[1], new Color(r, g, b).getRGB());
				blockPriority.put(block.split("\\:")[1], Integer.parseInt(block.split("\\:")[0]));
				
			}
			
		} else {

			ids.put(blocknames[0], id);
			blocks.put(blocknames[0], new Color(r, g, b).getRGB());
			blockPriority.put(blocknames[0], 1);
			
		}
		
	}
	
	public static void addBlockState(String blockID, String blockState) {
		
		blockStateNames.put(blockID, blockState);
		
	}
	
}
