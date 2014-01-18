package bytecodeTricks;

import java.util.ArrayList;

import battlecode.common.*;

public class RobotPlayer{
	
	public static void run(RobotController rc){
		double[][] cowGrowth = rc.senseCowGrowth();
		
		int startBytecode = Clock.getBytecodeNum();
		//USING ARRAYLISTS
//		ArrayList<MapLocation> goodLocations = new ArrayList<MapLocation>();
//		for(int x = 0;x<rc.getMapWidth();x++){
//			for(int y=0;y<rc.getMapHeight();y++){
//				if(cowGrowth[x][y]>1){
//					goodLocations.add(new MapLocation(x,y));
//				}
//			}
//		}
		
		//USING ARRAYLISTS
//		MapLocation[] greatLocations = new MapLocation[100000];
//		int index = 0;
//		for(int x = 0;x<rc.getMapWidth();x++){
//			for(int y=0;y<rc.getMapHeight();y++){
//				if(cowGrowth[x][y]>1){
//					greatLocations[index]=new MapLocation(x,y);
//					index++;
//				}
//			}
//		}
		
		//SAVE GETMAPWIDTH and GETMAPHEIGHT
//		MapLocation[] greatLocations = new MapLocation[100000];
//		int index = 0;
//		int width = rc.getMapWidth();
//		int height = rc.getMapHeight();
//		for(int x = 0;x<width;x++){
//			for(int y=0;y<height;y++){
//				if(cowGrowth[x][y]>1){
//					greatLocations[index]=new MapLocation(x,y);
//					index++;
//				}
//			}
//		}
//		
		
		MapLocation[] greatLocations = new MapLocation[100000];
		int index = 0;
		int width = rc.getMapWidth();
		int height = rc.getMapHeight();
		for(int x = width;--x>=0;){
			for(int y=height;--y>=0;){
				if(cowGrowth[x][y]>1){
					greatLocations[index]=new MapLocation(x,y);
					index++;
				}
			}
		}
		
		int bytecodesThisRound = (Clock.getBytecodeNum()-startBytecode);
		int roundBytecodes = Clock.getRoundNum();
		if(bytecodesThisRound<0){
			bytecodesThisRound+=10000;
			roundBytecodes-=1;
		}
		System.out.println("used "+roundBytecodes+""+bytecodesThisRound+" bytecodes");
		rc.yield();
	}
	
}