package sprintTourney;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer{

	public static RobotController rc;							//We know what this is
	static Direction allDirections[] = Direction.values();		//randomly selects direction
	static Random rng = new Random();							//used for Random Number Generation (R-N-G)
	static int directionalLooks[] = new int[]{0,1,-1,2,-2};

	public static double pastrSpawnChance = 0.0001;				//Possibly used to change later in code
	public static int counter = 0;								//Counter for various purposes (none in use now)
	public static int randAI = 0;						//Used to prevent super linear AI (straight shots OR stuck on HQ)

	public static void run(RobotController rc_in)
	{

		rc = rc_in;
		rng.setSeed(rc.getRobot().getID());	//random AI based on Robot's unique ID
		while(true)
		{
			try{

				if(rc.getType()==RobotType.HQ)	//Existential HQ (Who am I?)
				{//if I'm a headquarters
					runHeadquarters();
				}else if(rc.getType()==RobotType.SOLDIER)	//Thick Headed Soldier (Where do I go?)
				{
					runSoldier();
				}
				rc.yield();

			}catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void runSoldier() throws GameActionException 
	{		
		tryToShoot();
		randomMove();
	}

	private static void randomMove() throws GameActionException
	{
		randAI = (int)(rng.nextDouble() * 10);
		Direction d = allDirections[(int)(rng.nextDouble() * 8)];					//Randomly chosen Direction
		Direction h = rc.getLocation().directionTo(rc.senseEnemyHQLocation());		//Direction of Enemy HQ

		if (randAI < 2 && rc.isActive() && rc.canMove(d))			//Can we move?  (random direction ~20%)
		{
			rc.move(d);		//Yup!
		}else if (randAI >= 2 && rc.isActive() && rc.canMove(h))	//Can we charge?(Charge HQ ~80%)
		{
			rc.sneak(h);	//Yup!
		}
	}

	private static void tryToShoot() throws GameActionException 
	{
		//shooting
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,10000,rc.getTeam().opponent());	//store enemy data in an array
		if(enemyRobots.length > 0)							//if there are enemies in range
		{
			Robot anEnemy = enemyRobots[0];					//pick the first robot
			RobotInfo anEnemyInfo;							//gather his information to shoot at location
			anEnemyInfo = rc.senseRobotInfo(anEnemy);		//Convert data to one that can obtain location
			if(anEnemyInfo.location.distanceSquaredTo(rc.getLocation()) < rc.getType().attackRadiusMaxSquared)	//Gain location and check distance
			{
				if(rc.isActive())	//Can we attack?
				{
					rc.attackSquare(anEnemyInfo.location);		//Yup!
				}
			}
		}else	//there are no enemies around, so build a PASTR
		{
			if(rng.nextDouble() < pastrSpawnChance && rc.sensePastrLocations(rc.getTeam()).length < 5) //prevent spawning too many PASTRS
			{				
				if(rc.isActive()) 		//can we build a PASTR?
				{					
					rc.construct(RobotType.PASTR);	//Yup!
				}				
			}
		}
	}

	private static void runHeadquarters() throws GameActionException 
	{
		Direction spawnDir = allDirections[(int)(rng.nextDouble() * 8)];	//pick a random spawn direction
		if(rc.isActive() && rc.canMove(spawnDir) && rc.senseRobotCount() < GameConstants.MAX_ROBOTS)	//usual checks (can we spawn)
		{
			rc.spawn(spawnDir);	//Spawn in the random direction
		}
	}
}