using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Spawner : MonoBehaviour {

	public ObstacleMove[] prefabs;
	public int poolSize;
	public float spawnXPos;

    public float spawnMinY;
    public float spawnMaxY;

	public float spawnInterval;
	private float timer;

	private LinkedList<ObstacleMove> pool; //queue used to hold obstacles

	// Use this for initialization
	void Start () {

		//create the pool and populate it
		pool = new LinkedList<ObstacleMove> ();
		for(int i = 0; i < poolSize; i++)
		{
			int rNum = Random.Range(0, prefabs.Length);//random item from the array
			ObstacleMove clone = Instantiate<ObstacleMove>(prefabs[rNum]);

			clone.gameObject.SetActive(false);//keep inactive while cached
			clone.transform.SetParent(this.transform);//make this our child
			pool.AddLast(clone);//add to pool
		}
	}
	
	// Update is called once per frame
	void Update ()
    {
        //spawn every interval
        timer += Time.deltaTime;
        if(timer >= spawnInterval)
        {
            Spawn();
            timer -= spawnInterval;
        }	
	}

    //get an item from the pool and spawn it
    void Spawn()
    {
        if (pool.Count == 0)
        {
            Debug.Log("Nothing left to spawn...");
            return;
        }

        //spawn next item
        ObstacleMove next = pool.First.Value;

        //create random height
        float height = Random.Range(spawnMinY, spawnMaxY); //this is the offset where we expect walls to be spawned in

        System.Random r = new System.Random(); //wanted to be able to get a random int
        int randomNeg = (r.Next(2) * 2) - 1; //this randomly returns -1, or 1    p.s. r.Next(inclusive, exclusive) 

        next.homePoint.y = height * randomNeg;

        //Debug.Log("height = " + height);//making sure our values come out randomly
        //Debug.Log("randomNeg = " + randomNeg);
        //Debug.Log("Y homepoint = " + next.homePoint.y);

        next.transform.position = new Vector3(spawnXPos, next.homePoint.y); //we have adjusted this height
        next.gameObject.SetActive(true);

        pool.RemoveFirst();
    }

    void Recycle(GameObject garbage)
    {
        ObstacleMove mover = garbage.GetComponent<ObstacleMove>();
        if(mover == null)
        {
            Debug.Log("Could not add " + garbage.name);
            return;
        }

        garbage.SetActive(false);//turn it off
        pool.AddLast(mover); //enque to pool
    }

    void OnTriggerEnter2D(Collider2D other)
    {
        if(other.CompareTag("Wall"))
        {
            Recycle(other.gameObject);
        }
    }
}
