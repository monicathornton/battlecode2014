//code from lecture 1
package samplePlayer;

import battlecode.common.*;

public class RobotPlayer{
	
	public static void run(RobotController rc){
		while(true){
			if(rc.getType()==RobotType.HQ){//if I'm a headquarters
				Direction spawnDir = Direction.NORTH;
				try {
					if(rc.isActive()&&rc.canMove(spawnDir)&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
						rc.spawn(Direction.NORTH);
					}
				} catch (GameActionException e) {
					// TODO hi contestant who downloaded this.
					e.printStackTrace();
				}
			}else if(rc.getType()==RobotType.SOLDIER){
				//shooting
				Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
				if(enemyRobots.length>0){//if there are enemies
					Robot anEnemy = enemyRobots[0];
					RobotInfo anEnemyInfo;
					try {
						anEnemyInfo = rc.senseRobotInfo(anEnemy);
						if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared){
							if(rc.isActive()){
								rc.attackSquare(anEnemyInfo.location);
							}
						}
					} catch (GameActionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{//there are no enemies, so build a tower
					if(Math.random()<0.01){
						if(rc.isActive()){
							try {
								rc.construct(RobotType.PASTR);
							} catch (GameActionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				//movement
				Direction allDirections[] = Direction.values();
				Direction chosenDirection = allDirections[(int)(Math.random()*8)];
				if(rc.isActive()&&rc.canMove(chosenDirection)){
					try {
						rc.move(chosenDirection);
					} catch (GameActionException e) {
						e.printStackTrace();
					}
				}
			}
			rc.yield();
		}
	}
}