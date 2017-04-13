using UnityEngine;
using System.Collections;

//Part is another serializable data storage class just like WeaponDefinition
[System.Serializable]
public class Part
{
    //These three fields need to be defined in the Inspector pane
    public string name; //The name of this part
    public float health; //The amount of health this part has
    public string[] protectedBy; //The other parts that protect this

    //These two fields are set automatically in Start()
    //Caching like this makes it faster and easier to find these later
    public GameObject go; //The gameObject for this part
    public Material mat; //The Material to show damage
}

public class Enemy_4 : Enemy {
    //Enemy 4 will start offscreen and then pick a random point on screen to 
    //move to. Once it has arrived, it will pick another random point and
    //continue until the player has shot it down.

    public Vector3[] points; //stores the p0 & p1 for interpolation
    public float timeStart; //BirthTime for this Enemy_4
    public float duration = 4;//duration of movement

    public Part[] parts; //The array of ship parts

	// Use this for initialization
	void Start ()
    {
        points = new Vector3[2];
        //There is already an initial position chosen by Main.SpawnEnemy()
        //so add it to points as the initial p0 & p1
        points[0] = pos;
        points[1] = pos;

        InitMovement();

        //Cashe GameObject & Material of each Part in parts
        Transform t;
        foreach(Part prt in parts)
        {
            t = transform.Find(prt.name);
            if(t != null)
            {
                prt.go = t.gameObject;
                prt.mat = prt.go.GetComponent<Renderer>().material;
            }
        }
	}
	
	void InitMovement()
    {
        //Pick a new point to move to that is on screen
        Vector3 p1 = Vector3.zero;
        float esp = Main.S.enemySpawnPadding;
        Bounds cBounds = Utils.camBounds;
        p1.x = Random.Range(cBounds.min.x + esp, cBounds.max.x - esp);
        p1.y = Random.Range(cBounds.min.y + esp, cBounds.max.y - esp);

        points[0] = points[1]; //shift points[1] to points[0]
        points[1] = p1; //add pi as points[1]

        //reset the time
        timeStart = Time.time;
    }

    //move
    public override void Move()
    {
        float u = (Time.time - timeStart) / duration;
        if(u >= 1)
        {
            InitMovement();
            u = 0;
        }

        u = 1 - Mathf.Pow(1 - u, 2);

        pos = (1 - u) * points[0] + u * points[1];
    }

    //This will override the OnCollisionEnter that is part of Enemy.cs
    //Because of the way that MonoBehavior declares common Unity functions
    //like OnCollisionEnter(), that override keyword is not necessary.
    void OnCollisionEnter(Collision coll)
    {
        GameObject other = coll.gameObject;
        switch(other.tag)
        {
            case "ProjectileHero":
                Projectile p = other.GetComponent<Projectile>();
                //Enemies don't take damage unless they're on screen
                //This stops the player from shooting them before they are visible
                bounds.center = transform.position + boundsCenterOffset;
                if(bounds.extents == Vector3.zero || Utils.ScreenBoundsCheck(bounds, BoundsTest.offscreen) != Vector3.zero)
                {
                    Destroy(other);
                    break;
                }

                //Hurt this enemy
                //find the gameobject that was hit
                //the collision coll has contacts[], an array of ContactPoints
                //because there was a collision, we're guaranteed that there is at 
                //least a contacts[0], and ContactPoints have a reference to
                //this collider, which will be the collider for the part of the 
                //Enemy_4 that was hit
                GameObject goHit = coll.contacts[0].thisCollider.gameObject;
                Part prtHit = FindPart(goHit);
                if(prtHit == null)
                {
                    //if prtHit wasn't found
                    //then it is usually because, very rarely, thisCollider on
                    //contacts[0] will be the ProjectileHero instead of the ship
                    //part. If so, just look for the other collider instead
                    goHit = coll.contacts[0].otherCollider.gameObject;
                    prtHit = FindPart(goHit);
                }

                //check whether this part is still protected
                if(prtHit.protectedBy != null)
                {
                    foreach(string s in prtHit.protectedBy)
                    {
                        //if one of the protecting parts hasn't been destroyed
                        if(!Destroyed(s))
                        {
                            //then don't damage this part yet
                            Destroy(other); //destroy the ProjectileHero
                            return;//return before causing damage
                        }
                    }
                }

                //It's not protected, so make it take damage
                //get the damage amount from the Projectile.type * Main.W_DEFS
                prtHit.health -= Main.W_DEFS[p.type].damageOnHit;

                //show damage on the part
                ShowLocalizedDamage(prtHit.mat);
                if(prtHit.health <= 0)
                {
                    //Instead of destroying this enemy, disable the damaged part
                    prtHit.go.SetActive(false);
                }

                //check to see if the whole ship is destroyed
                bool allDestroyed = true; //assume it is destroyed
                foreach(Part prt in parts)
                {
                    if(!Destroyed(prt))
                    {
                        //if a part still exists
                        allDestroyed = false; //change all destroyed back to false
                        break; //and break out of the foreach loop
                    }
                }

                if(allDestroyed)
                {
                    //it is completely destroyed
                    //The the Main singleton that this ship has been destroyed
                    Main.S.ShipDestroyed(this);

                    //destroy this gameObject
                    Destroy(this.gameObject);
                }

                Destroy(other);
                break;
        }
    }

    //These two functions find a Part in this.parts by name or gameObject
    Part FindPart(string n)
    {
        foreach(Part prt in parts)
        {
            if(prt.name == n)
            {
                return (prt);
            }
        }
        return null;
    }

    Part FindPart(GameObject go)
    {
        foreach(Part prt in parts)
        {
            if(prt.go == go)
            {
                return (prt);
            }
        }
        return null;
    }

    //These functions return true if the Part has been destroyed
    bool Destroyed(GameObject go)
    {
        return (Destroyed(FindPart(go)));
    }

    bool Destroyed(string n)
    {
        return (Destroyed(FindPart(n)));
    }

    bool Destroyed(Part prt)
    {
        if(prt == null)
        {
            //if no real part was passed in
            return (true); //yes, it was destroyed
        }

        //returns the result of the comparison: prt.health <= 0
        //If prt.health is 0 or less, return true (yes it was destroyed)
        return (prt.health <= 0);
    }

    //This changes the color of just one part to red instead of the whole ship
    void ShowLocalizedDamage(Material m)
    {
        m.color = Color.red;
        remainingDamageFrames = showDamageForFrames;
    }
}
