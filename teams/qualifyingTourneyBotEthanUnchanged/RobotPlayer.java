package qualifyingTourneyBotEthanUnchanged;

//things to do:
//defend pastrs that are under attack, or at least consider defending them
//battlecry when charging into battle -> concerted effort
//something like the opposite of a battlecry, when you're sure you're outnumbered

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

	public static void run(RobotController rcIn) throws GameActionException{
		rc=rcIn;
		Comms.rc = rcIn;
		randall.setSeed(rc.getRobot().getID());
		enemyHQ = rc.senseEnemyHQLocation();

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
			}catch (Exception e){
				e.printStackTrace();
			}
			rc.yield();
		}
	}

	private static void runHQ() throws GameActionException {
		//look for enemies near base
		// commented out by Ethan
//		enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000, rc.getTeam().opponent());
////		if(enemyRobots[0]!=null){// if an enemy has been found...
////			HQAttack(enemyRobots);
////		}
//		int checker = 0; // looks though the array of possible enemy robots to see if we see one
//		while(checker < enemyRobots.length){
//			if(enemyRobots[checker]!=null){// if an enemy has been found...
//				HQAttack(enemyRobots); // Attack!
//			}
//			enemyRobots[checker] = null; // resets the value so we only have up-to-date info in enemyRobots
//		}
		//TODO consider updating the rally point to an allied pastr 

		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000000,rc.getTeam());

		//if my team is defeated, regroup at main base:
		if (Clock.getRoundNum() <= 400) {
			//milkHunt();
			MapLocation startPoint = findAverageAllyLocation(alliedRobots);			
			Comms.findPathAndBroadcast(2, startPoint,rc.senseHQLocation(), bigBoxSize, 2);
		}
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
		Robot[] targets = rc.senseNearbyGameObjects(Robot.class,1000000000,rc.getTeam().opponent());
		if (targets.length > 0) {
			RobotInfo robotInfo = rc.senseRobotInfo(targets[0]);
			rc.attackSquare(robotInfo.location);
		}
		// commented out by ethan
//		MapLocation[] enemyRobotLocations = VectorFunctions.robotsToLocations(targets, rc, true);
//		
//		MapLocation closestEnemyLoc = VectorFunctions.findClosest(enemyRobotLocations, rc.getLocation());
//		boolean closeEnoughToShoot = closestEnemyLoc.distanceSquaredTo(rc.getLocation())<=rc.getType().attackRadiusMaxSquared;
//		attackClosest(closestEnemyLoc);
//		Robot target;
//		int i = 0;
//		while(i < targets.length){
//			if(targets[i]!=null){ // if there is a enemy...
//				target = targets[i];
//				Robot shotIt = getRobot(target);
//				canAttackSquare(getLocation(target));
//			}
//		}
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
				//mt added
				if((alliedRobots.length+1)>=enemyRobots.length && closeEnoughToShoot){//attack when you have superior numbers
					attackClosest(closestEnemyLoc);
				}else{//otherwise regroup					
					regroup(enemyRobots,alliedRobots,closestEnemyLoc);
				} 
			}
		}else{//NAVIGATION BY DOWNLOADED PATH
			navigateByPath(alliedRobots);
		//Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		//BasicPathing.tryToMove(towardEnemy, true, rc, directionalLooks, allDirections);//was Direction.SOUTH_EAST

		//Direction towardEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		//simpleMove(towardEnemy);
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
		private static void milkHunt() {
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
		}	
		}




//	private static MapLocation getRandomLocation() {
//		return new MapLocation(randall.nextInt(rc.getMapWidth()),randall.nextInt(rc.getMapHeight()));
//	}
//
//	private static void simpleMove(Direction chosenDirection) throws GameActionException{
//		if(rc.isActive()){
//			for(int directionalOffset:directionalLooks){
//				int forwardInt = chosenDirection.ordinal();
//				Direction trialDir = allDirections[(forwardInt+directionalOffset+8)%8];
//				if(rc.canMove(trialDir)){
//					rc.move(trialDir);
//					break;
//				}
//			}
//		}
//	}

}