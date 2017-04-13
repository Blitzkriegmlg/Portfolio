using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Main : MonoBehaviour {

    static public Main S;

    static public Dictionary<WeaponType, WeaponDefinition> W_DEFS;

    public GameObject[] prefabEnemies;
    public float enemySpawnPerSecond = 0.5f;
    public float enemySpawnPadding = 1.5f; //padding for position

    public WeaponDefinition[] weaponDefinitions;
    public GameObject prefabPowerUp;
    public WeaponType[] powerUpFrequency = new WeaponType[]
                { WeaponType.blaster, WeaponType.blaster, WeaponType.spread, WeaponType.shield };

    public bool ____________________;

    public WeaponType[] activeWeaponTypes;
    public float enemySpawnRate; //delay between enemy spawns

    void Awake()
    {
        S = this;

        //set utils.camBounds
        Utils.SetCameraBounds(this.GetComponent<Camera>());

        //0.5 enemies/second = enemySpawnRate of 2
        enemySpawnRate = 1f / enemySpawnPerSecond;

        //invoke call SpawnEnemy() one after a 2 second delay
        Invoke("SpawnEnemy", enemySpawnRate);

        W_DEFS = new Dictionary<WeaponType, WeaponDefinition>();
        foreach(WeaponDefinition def in weaponDefinitions)
        {
            W_DEFS[def.type] = def;
        }
    }

    static public WeaponDefinition GetWeaponDefinition(WeaponType wt)
    {
        //check to make sure that they key exists in the Dictionary
        //attempting to retrieve a key that didn't exist would throw an error,
        //so the following if statement is important
        if(W_DEFS.ContainsKey(wt))
        {
            return (W_DEFS[wt]);
        }
        //this will return a definition for WeaponType.none,
        //which means it has failed to find the weapon definition
        return (new WeaponDefinition());
    }

    void Start()
    {
        activeWeaponTypes = new WeaponType[weaponDefinitions.Length];
        for(int i = 0; i < weaponDefinitions.Length; i++)
        {
            activeWeaponTypes[i] = weaponDefinitions[i].type;
        }
    }

	public void SpawnEnemy()
    {
        //pick a random enemy prefab to instantiate
        int ndx = Random.Range(0, prefabEnemies.Length);
        GameObject go = Instantiate(prefabEnemies[ndx]) as GameObject;

        //position the enemy above the screen with a random x position
        Vector3 pos = Vector3.zero;
        float xMin = Utils.camBounds.min.x + enemySpawnPadding;
        float xMax = Utils.camBounds.max.x - enemySpawnPadding;

        pos.x = Random.Range(xMin, xMax);
        pos.y = Utils.camBounds.max.y + enemySpawnPadding;

        go.transform.position = pos;

        //call SpawnEnemy() again in a couple of seconds
        Invoke("SpawnEnemy", enemySpawnRate);
    }

    public void DelayedRestart(float delay)
    {
        //invoke the Restart() method in delay seconds
        Invoke("Restart", delay);
    }

    public void Restart()
    {
        //reload Scene_0 to restart the game
        Application.LoadLevel("Scene_0");
    }

    public void ShipDestroyed(Enemy e)
    {
        //Potentially generate a powerUp
        if(Random.value <= e.powerUpDropChange)
        {
            //random.value generates a value between 0 and 1 though never 1
            //if the e.powerUpDropChange is .5f, a PowerUp will be generated 
            //50% of the time. For testing, it's now set to 1f.

            //choose which PowerUp to pick
            //Pick one from the possibilities in powerUpFrequency
            int ndx = Random.Range(0, powerUpFrequency.Length);
            WeaponType puType = powerUpFrequency[ndx];

            //spawn a powerUp
            GameObject go = Instantiate(prefabPowerUp) as GameObject;
            PowerUp pu = go.GetComponent<PowerUp>();
            //set it to the proper WeaponType
            pu.SetType(puType);

            //set it to the position of the destroyed ship
            pu.transform.position = e.transform.position;
        }
    }
}
