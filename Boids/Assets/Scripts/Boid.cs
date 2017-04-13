using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class Boid : MonoBehaviour {
    //this static list holds all Boid instances and is shared amongst them
    static public List<Boid> boids;

    //this object does not have a rigidbody so we handle velocity directly

    public Vector3 velocity; //The current velocity
    public Vector3 newVelocity; // The velocity for the next frame
    public Vector3 newPosition; //The position for the next frame

    public List<Boid> neighbors; //All nearby Boids
    public List<Boid> collisionRisks; //All boids that are too close
    public Boid closest; //the closest single boid


	void Awake()
    {
        //Define the boids List if it is still null
        if(boids == null)
        {
            boids = new List<Boid>();
        }

        //Add this boid to boids so that we can collect each one there
        boids.Add(this);

        //Give this boid a random position and velocity
        Vector3 randPos = Random.insideUnitSphere * BoidSpawner.S.spawnRadius;
        randPos.y = 0; //Flatten the boid to move only in the XZ plane
        this.transform.position = randPos;
        velocity = Random.onUnitSphere;
        velocity *= BoidSpawner.S.spawnVelocity;

        Debug.Log("velocity = " + velocity); //I'm getting 0 initial velocity right now
  

        //Initialize the two Lists
        neighbors = new List<Boid>();
        collisionRisks = new List<Boid>();

        //make this.transform a child of the Boids GameObject, just for clarity in the hiearchy view
        this.transform.parent = GameObject.Find("Boids").transform;

        //Give this boid a random color, but not one that is too dark
        Color randColor = Color.black;
        while(randColor.r + randColor.g + randColor.b < 1.0f)
        {
            randColor = new Color(Random.value, Random.value, Random.value);
        }
        Renderer[] rends = gameObject.GetComponentsInChildren<Renderer>();
        foreach(Renderer r in rends)
        {
            r.material.color = randColor;
        }
    }//end Awake

    void Update()
    {
        //get the list of nearby boids
        List<Boid> neighbors = GetNeighbors(this);

        //Initialize newVelocity and newPosition to the current values
        newVelocity = velocity;
        newPosition = this.transform.position;

        //velocity matching: this sets the velocity of the boid
        //to be similar to that of its neighbors
        Vector3 neighborVel = GetAverageVelocity(neighbors);

        //utilizes the fields set on the BoidSpawner.S singleton
        newVelocity += neighborVel * BoidSpawner.S.VelocityMatchingAmt;

        //flock centering: move toward the middle of neighbors
        Vector3 neighborCenterOffset = GetAveragePosition(neighbors) - this.transform.position;
        newVelocity += neighborCenterOffset * BoidSpawner.S.flockCenteringAmt;

        //collision avoidance: avoid runing into Boids that are too close
        Vector3 dist;
        if(collisionRisks.Count > 0)
        {
            Vector3 collisionAveragePos = GetAveragePosition(collisionRisks);
            dist = collisionAveragePos - this.transform.position;
            newVelocity += dist * BoidSpawner.S.collisionAvoidanceAmt;
        }

        //mouse attraction: move toward the mouse no matter how far away
        dist = BoidSpawner.S.mousePos - this.transform.position;
        if(dist.magnitude > BoidSpawner.S.mouseAvoidanceDist)
        {
            newVelocity += dist * BoidSpawner.S.mouseAttractionAmt;
        } else
        {
            //if the mouse is too close, move away quickly
            newVelocity -= dist.normalized * BoidSpawner.S.mouseAvoidanceDist * BoidSpawner.S.mouseAvoidanceAmt;
        }
		Debug.Log("newVelocity = " + newVelocity); //I'm getting 0 initial velocity right now
        /*
            newVelocity and newPosition are ready, but we will wait until LateUpdate to move them so that
            this boid does not move before others have a chance to calculate thier new values.
        */

    }//end update

    /*
        By allowing all boids to Update() before any of them move, we
        avoid a race condition that could cause some boids moving 
        before others have decided where to go.
    */
    void LateUpdate()
    {
        //Adjust the current velocity based on newVelocity
        //using a linear interpolation
        velocity = (1 - BoidSpawner.S.velocityLerpAmt) * velocity +
            BoidSpawner.S.velocityLerpAmt * newVelocity;

        //make sure the velocity is within the max and min limits
        if(velocity.magnitude > BoidSpawner.S.maxVelocity)
        {
            velocity = velocity.normalized * BoidSpawner.S.maxVelocity;
        }
        if(velocity.magnitude < BoidSpawner.S.minVelocity)
        {
            velocity = velocity.normalized * BoidSpawner.S.minVelocity;
        }

        //decide on the newPosition
        newPosition = this.transform.position + velocity * Time.deltaTime;

        //keep everything in the XZ plane
        newPosition.y = 0;

        //turn toward the newPosition to orient our model
        this.transform.LookAt(newPosition);

		Debug.Log("newPosition = " + newPosition); //Something isn't right, they don't move

        //actually move into position
        this.transform.position = newPosition;

    }//end LateUpdate

    //helper methods

    /*
        Find which boids are near enough to be neighbors.
        boid is BoidOfInterest, the Boid on which we're focusing
    */
    public List<Boid> GetNeighbors(Boid boi)
    {
        float closestDist = float.MaxValue; // the max value any float can hold
        Vector3 delta;
        float dist;
        neighbors.Clear();
        collisionRisks.Clear();

        foreach( Boid b in boids)
        {
            if (b == boi) continue;

            delta = b.transform.position - boi.transform.position;
            dist = delta.magnitude;

            if(dist < closestDist)
            {
                closestDist = dist;
                closest = b;
            }

            if(dist < BoidSpawner.S.nearDist)
            {
                neighbors.Add(b);
            }

            if(dist < BoidSpawner.S.collisionDist)
            {
                collisionRisks.Add(b);
            }

        }

        if(neighbors.Count == 0)
        {
            neighbors.Add(closest);
        }

        return (neighbors);

    }

    //Get the average psoition of the Boids in a List<Boid>
    public Vector3 GetAveragePosition(List<Boid> someBoids)
    {
        Vector3 sum = Vector3.zero;
        foreach(Boid b in someBoids)
        {
            sum += b.transform.position;
        }
        Vector3 center = sum / someBoids.Count;
        return (center);
    }

    //Get the average velocity of the Boids in a list<Boid> 
    public Vector3 GetAverageVelocity(List<Boid> someBoids)
    {
        Vector3 sum = Vector3.zero;
        foreach(Boid b in someBoids)
        {
            sum += b.velocity;
        }

        Vector3 avg = sum / someBoids.Count;
        return (avg);
    }

}
