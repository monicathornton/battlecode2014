package qualifyingTourney;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;


public class RobotPlayer{

	public static RobotController rc;
	public static Direction allDirections[] = Direction.values();
	static Random randall = new Random();
	public static int directionalLooks[] = new int[]{0,1,-1,2,-2,3,-3,4};
	static ArrayList<MapLocation> path = new ArrayList<MapLocation>();
	static int bigBoxSize = 5;
	static MapLocation enemyHQ;

	//HQ data:
	static MapLocation rallyPoint;
	static MapLocation targetedPastr;
	static boolean die = false;
	static Robot[] enemyRobots = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};

	//SOLDIER data:
	static int myBand = 100;
	static int pathCreatedRound = -1;

	//TOWER data:
	static MapLocation pastrs[];	
	static MapLocation target;		

	static Random rng = new Random();	//used for Random Number Generation (R-N-G)		

	static int d = 0;

	static boolean tower = false;		//Used for soldiers to turn into towers
	static boolean pastr = false;		//Used for soldiers to turn into pastrs
	static boolean init = true;
	static boolean deny = false;
	static int count = 0;
	static int spawnC = 1;
	static int totalPASTRs = 0;
	static int totalTowers = 0;

	public static void run(RobotController rcIn) throws GameActionException{
		rc=rcIn;
		Comms.rc = rcIn;
		randall.setSeed(rc.getRobot().getID());
		enemyHQ = rc.senseEnemyHQLocation();

		//for running the noise tower
//		target = rc.getLocation();
//		target = target.add(allDirections[d], 17);

		if(rc.getType()==RobotType.HQ){
			rc.broadcast(101,VectorFunctions.locToInt(VectorFunctions.mldivide(rc.senseHQLocation(),bigBoxSize)));//this tells soldiers to stay near HQ to start
			rc.broadcast(102,-1);//and to remain in squad 1
			tryToSpawn();
			BreadthFirst.init(rc, bigBoxSize);
			rallyPoint = VectorFunctions.mladd(VectorFunctions.mldivide(VectorFunctions.mlsubtract(rc.senseEnemyHQLocation(),rc.senseHQLocation()),3),rc.senseHQLocation());
		}else{
			BreadthFirst.rc=rcIn;//slimmed down init
		}

		while(true){
			try{
				if(rc.getType()==RobotType.HQ){
					HQAttack(); // try to attack first
					runHQ();
					milkHunt();
					if(die){
						break;
					}
					rc.yield();
				}else if(rc.getType()==RobotType.SOLDIER){
					runSoldier();
					rc.yield();
				} 
//				else if(rc.getType() == RobotType.NOISETOWER){
//					towerTime();
//					runNoiseTower();
//				}
			}catch (Exception e){
				e.printStackTrace();
			}
			rc.yield();
		}
	}

	private static void runHQ() throws GameActionException {
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000000,rc.getTeam());


		//if my team is defeated, regroup at main base:
		if(Clock.getRoundNum()>400&&alliedRobots.length<5){//call a retreat
			MapLocation startPoint = findAverageAllyLocation(alliedRobots);
			Comms.findPathAndBroadcast(2,startPoint,rc.senseHQLocation(),bigBoxSize,2);
			rallyPoint = rc.senseHQLocation();
		}else{//not retreating
			//tell them to go to the rally point
			Comms.findPathAndBroadcast(1,rc.getLocation(),rallyPoint,bigBoxSize,2);

			//if the enemy builds a pastr, tell sqaud 2 to go there.
			MapLocation[] enemyPastrs = rc.sensePastrLocations(rc.getTeam().opponent());
			MapLocation[] alliedPastrs =rc.sensePastrLocations(rc.getTeam());

			if ((alliedPastrs.length > 0) && ((enemyPastrs.length - alliedPastrs.length) > 0)) {
				//protect our pastrs
				MapLocation startPoint = findAverageAllyLocation(alliedRobots);
				targetedPastr = getNextTargetPastr(enemyPastrs,startPoint);
				//broadcast it
				Comms.findPathAndBroadcast(2,startPoint,targetedPastr,bigBoxSize,2);
			} else if (alliedPastrs.length>0) {
				MapLocation startPoint = findAverageAllyLocation(alliedRobots);
				targetedPastr = getNextTargetPastr(alliedPastrs,startPoint);
				//broadcast it
				Comms.findPathAndBroadcast(2,startPoint,targetedPastr,bigBoxSize,2);	
			} else if (enemyPastrs.length>0){
				MapLocation startPoint = findAverageAllyLocation(alliedRobots);
				targetedPastr = getNextTargetPastr(enemyPastrs,startPoint);
				//broadcast it
				Comms.findPathAndBroadcast(2,startPoint,targetedPastr,bigBoxSize,2);
			} 
		}

		//consider attacking
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		if(rc.isActive()&&enemyRobots.length>0){
			MapLocation[] enemyRobotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc, true);
			MapLocation closestEnemyLoc = VectorFunctions.findClosest(enemyRobotLocations, rc.getLocation());
			if(rc.canAttackSquare(closestEnemyLoc))
				rc.attackSquare(closestEnemyLoc);
		}
		//after telling them where to go, consider spawning
		tryToSpawn();
	}

	private static MapLocation findAverageAllyLocation(Robot[] alliedRobots) throws GameActionException {
		//find average soldier location
		MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
		MapLocation startPoint;
		if(alliedRobotLocations.length>0){
			startPoint = VectorFunctions.meanLocation(alliedRobotLocations);
			if(Clock.getRoundNum()%100==0)//update rally point from time to time
				rallyPoint=startPoint;
		}else{
			startPoint = rc.senseHQLocation();
		}
		return startPoint;
	}

	private static MapLocation getNextTargetPastr(MapLocation[] enemyPastrs,MapLocation startPoint) {
		if(enemyPastrs.length==0)
			return null;
		if(targetedPastr!=null){//a targeted pastr already exists
			for(MapLocation m:enemyPastrs){//look for it among the sensed pastrs
				if(m.equals(targetedPastr)){
					return targetedPastr;
				}
			}
		}//if the targeted pastr has been destroyed, then get a new one
		return VectorFunctions.findClosest(enemyPastrs, startPoint);
	}

	private static void HQAttack() throws GameActionException { // a method of attack for the HQ, does not work yet... will try again...
		//		Robot[] targets = rc.senseNearbyGameObjects(Robot.class,1000000000,rc.getTeam().opponent());
		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam().opponent());
		//		if (targets.length > 0) {
		//			RobotInfo robotInfo = rc.senseRobotInfo(targets[0]);
		//			//mt added to help with exceptions
		//			if (rc.canAttackSquare(robotInfo.location)) {
		//				rc.attackSquare(robotInfo.location);
		//			}
		//		}
		if (enemies.length > 0) {
			Robot anEnemy = enemies[0];
			RobotInfo anEnemyInfo;
			try {
				anEnemyInfo = rc.senseRobotInfo(anEnemy);
				if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation())<rc.getType().attackRadiusMaxSquared) {
					if(rc.isActive()) {
						rc.attackSquare(anEnemyInfo.location);
					}
				}
			}
			catch (Exception e) {
				System.out.println(e);
			}
		}
	}



	public static void tryToSpawn() throws GameActionException {
		if(rc.isActive()&&rc.senseRobotCount()<GameConstants.MAX_ROBOTS){
			for(int i=0;i<8;i++){
				Direction trialDir = allDirections[i];
				if(rc.canMove(trialDir)){
					rc.spawn(trialDir);
					break;
				}
			}
		}
	}

	private static void runSoldier() throws GameActionException {
		//follow orders from HQ
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,rc.getType().sensorRadiusSquared*2,rc.getTeam());//was 
		if(enemyRobots.length>0){//SHOOT AT, OR RUN TOWARDS, ENEMIES
			MapLocation[] enemyRobotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc, true);
			if(enemyRobotLocations.length==0){//only HQ is in view
				navigateByPath(alliedRobots);
			}else{//shootable robots are in view
				MapLocation closestEnemyLoc = VectorFunctions.findClosest(enemyRobotLocations, rc.getLocation());
				boolean closeEnoughToShoot = closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared;
				if((alliedRobots.length+1)>=enemyRobots.length && closeEnoughToShoot){//attack when you have superior numbers
					attackClosest(closestEnemyLoc);
				}else{//otherwise regroup					
					regroup(enemyRobots,alliedRobots,closestEnemyLoc);
				} 
			}
		}else{
			MapLocation[] alliedPastrs =rc.sensePastrLocations(rc.getTeam());
			MapLocation startPoint = findAverageAllyLocation(alliedRobots);
			if (alliedPastrs.length > 0) {
				targetedPastr = getNextTargetPastr(alliedPastrs,startPoint);
//				rc.construct(RobotType.NOISETOWER);
			}
			//NAVIGATION BY DOWNLOADED PATH
			navigateByPath(alliedRobots);
		}
	}

	private static void navigateByPath(Robot[] alliedRobots) throws GameActionException{
		if(path.size()<=1){//
			//check if a new path is available
			int broadcastCreatedRound = rc.readBroadcast(myBand);
			if(pathCreatedRound<broadcastCreatedRound){//download new place to go
				pathCreatedRound = broadcastCreatedRound;
				path = Comms.downloadPath();
			}else{//just waiting around. Consider building a pastr
				considerBuildingPastr(alliedRobots);
			}
		}
		if(path.size()>0){
			//follow breadthFirst path...
			Direction bdir = BreadthFirst.getNextDirection(path, bigBoxSize);
			//...except if you are getting too far from your allies
			MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(alliedRobots, rc, true);
			if(alliedRobotLocations.length>0){
				MapLocation allyCenter = VectorFunctions.meanLocation(alliedRobotLocations);
				if(rc.getLocation().distanceSquaredTo(allyCenter)>16){
					bdir = rc.getLocation().directionTo(allyCenter);
				}
			}
			BasicPathing.tryToMove(bdir, true,true, false);
		}
	}

	private static void considerBuildingPastr(Robot[] alliedRobots) throws GameActionException {
		if(alliedRobots.length>4){//there must be allies nearby for defense
			MapLocation[] alliedPastrs =rc.sensePastrLocations(rc.getTeam());
			if(alliedPastrs.length<3&&(rc.readBroadcast(50)+60<Clock.getRoundNum())){//no allied robot can be building a pastr at the same time
				for(int i=0;i<20;i++){
					MapLocation checkLoc = VectorFunctions.mladd(rc.getLocation(),new MapLocation(randall.nextInt(8)-4,randall.nextInt(8)-4));
					if(rc.canSenseSquare(checkLoc)){
						double numberOfCows = rc.senseCowsAtLocation(checkLoc);
						if(numberOfCows>1000){//there must be a lot of cows there
							if(alliedPastrs.length==0){//there must not be another pastr nearby
								buildPastr(checkLoc);
							}else{
								MapLocation closestAlliedPastr = VectorFunctions.findClosest(alliedPastrs, checkLoc);
								if(closestAlliedPastr.distanceSquaredTo(checkLoc)>GameConstants.PASTR_RANGE*5){
									buildPastr(checkLoc);
								}
							}
						}
					}
				}
			}
		}
	}

	private static void buildPastr(MapLocation checkLoc) throws GameActionException {
		rc.broadcast(50, Clock.getRoundNum());
		for(int i=0;i<100;i++){//for 100 rounds, try to build a pastr
			if(rc.isActive()){
				if(rc.getLocation().equals(checkLoc)){
					rc.construct(RobotType.PASTR);
				}else{
					Direction towardCows = rc.getLocation().directionTo(checkLoc);
					BasicPathing.tryToMove(towardCows, true,true, true);
				}
			}
			rc.yield();
		}
	}

	private static void regroup(Robot[] enemyRobots, Robot[] alliedRobots,MapLocation closestEnemyLoc) throws GameActionException {
		int enemyAttackRangePlusBuffer = (int) Math.pow((Math.sqrt(rc.getType().attackRadiusMaxSquared)+1),2);
		if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=enemyAttackRangePlusBuffer){//if within attack range, back up
			Direction awayFromEnemy = rc.getLocation().directionTo(closestEnemyLoc).opposite();
			BasicPathing.tryToMove(awayFromEnemy, true,true,false);
		}else{//if outside attack range, group up with allied robots
			MapLocation[] alliedRobotLocations = VectorFunctions.robotsToLocations(enemyRobots, rc,false);
			MapLocation alliedRobotCenter = VectorFunctions.meanLocation(alliedRobotLocations);
			Direction towardAllies = rc.getLocation().directionTo(alliedRobotCenter);
			BasicPathing.tryToMove(towardAllies, true,true, false);
		}
	}

	private static void attackClosest(MapLocation closestEnemyLoc) throws GameActionException {
		//attacks the closest enemy or moves toward it, if it is out of range
		if(closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared){//close enough to shoot
			if(rc.isActive()){
				rc.attackSquare(closestEnemyLoc);
			}
		}else{//not close enough to shoot, so try to go shoot
			Direction towardClosest = rc.getLocation().directionTo(closestEnemyLoc);
			//simpleMove(towardClosest);
			BasicPathing.tryToMove(towardClosest, true,true, false);
		}
	}

	//find where milk is growing the most, head on over there!
	//if they build a pastr, send there
	private static void milkHunt() throws GameActionException {
		double[][] lotsOfCows = rc.senseCowGrowth();
		double mostCows = Double.MIN_VALUE;
		int xLoc = Integer.MAX_VALUE;
		int yLoc = Integer.MAX_VALUE;
		//mt added
		if (Clock.getRoundNum() > 300) {
			for (int outerLoop = 0; outerLoop < lotsOfCows.length; outerLoop++) {
				for (int innerLoop = 0; innerLoop < lotsOfCows[outerLoop].length; innerLoop++) {
					if (lotsOfCows[outerLoop][innerLoop] > mostCows) {
						mostCows = lotsOfCows[outerLoop][innerLoop];	
						xLoc = outerLoop;
						yLoc = innerLoop;
					}
				}
			}

			MapLocation theMostCows = new MapLocation(xLoc, yLoc);
			VectorFunctions.bigBoxCenter(theMostCows, pathCreatedRound);
			//mt added
			Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000000,rc.getTeam());
			navigateByPath(alliedRobots);
			//done mt add
		}	
	}

	private static void runNoiseTower() throws GameActionException
	{	
		if(tower == false){
			if (rc.getLocation().isAdjacentTo(target) || rc.senseTerrainTile(target) == TerrainTile.OFF_MAP){			
				if(d >= 7){
					d = 0;							
				}else{
					d++;
				}
				target = rc.getLocation();
				if(d%2 == 0)
					target = target.add(allDirections[d],17);
				if(d%2 != 0)
					target = target.add(allDirections[d], 12);
			}

			if(rc.isActive() && rc.canAttackSquare(target)){
				rc.attackSquare(target);
				target = target.subtract(allDirections[d]);
			}
		}
	}

	private static void towerTime() throws GameActionException 
	{	
		MapLocation[] alliedPastrs =rc.sensePastrLocations(rc.getTeam());
		if(alliedPastrs.length > 0)	{
			pastr = true;
		}

			//Information conversion to shoot nearby enemies, if there are enemies we can shoot
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,rc.getType().attackRadiusMaxSquared,rc.getTeam().opponent());
			if(enemyRobots.length > 0){			
				Robot anEnemy = enemyRobots[0];
				RobotInfo anEnemyInfo;

				try {
					anEnemyInfo = rc.senseRobotInfo(anEnemy);

					if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation()) < rc.getType().attackRadiusMaxSquared){					
						if(rc.isActive()){
							rc.attackSquare(anEnemyInfo.location);					
						}
					}
				} catch (GameActionException e) {
					e.printStackTrace();
				}
			}
			if((tower || pastr) && count < 70){
				//If we didn't shoot anything or find any nearby enemy pastrs, move in a random direction		
				Direction d = Direction.EAST;
				//Direction d = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if(rc.isActive() && rc.canMove(d))		
					rc.move(d);
				count++;
			}

			//Used for starting spawn of NoiseTower and Pastr		
//			if(tower && rc.isActive() && count >= 70){
//				if(totalTowers<1){
//					totalTowers = totalTowers + 1;
//					rc.construct(RobotType.NOISETOWER);
//				}
//			}else if(pastr && rc.isActive() && count >= 70){
//				//MT changed
//				//if(totalPASTRs<2){
//				//end change, change back if does not work
//				if(totalPASTRs < 1)
//					totalPASTRs = totalPASTRs + 1;
//					rc.construct(RobotType.PASTR);
//				}
			}
		}


