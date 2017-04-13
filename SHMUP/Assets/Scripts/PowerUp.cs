using UnityEngine;
using System.Collections;

public class PowerUp : MonoBehaviour {

    //this is an unusual but handly use of Vector2s. x holds a min value
    //and y a max value for a Random.Range() that will be called later
    public Vector2 rotMinMax = new Vector2(15, 90);
    public Vector2 driftMinMax = new Vector2(.25f, 2);
    public float lifeTime = 6f; //seconds the powerup exists

    public float fadeTime = 4f;//seconds it will then fade

    public bool _________________;

    public WeaponType type; //the type of the powerup
    public GameObject cube; //Reference to the cube child
    public TextMesh letter; //reference to the 3D letter
    public Vector3 rotPerSecond; //Euler rotation speed
    public float birthTime;

    void Awake()
    {
        //find the cube reference
        cube = transform.Find("Cube").gameObject;

        //find the TextMesh
        letter = GetComponent<TextMesh>();

        //set a random velocity
        Vector3 vel = Random.onUnitSphere; //get Random XYZ velocity
        //Random.onUnitSphere gives you a vector point that is whomewhere on
        //the surface of the sphere with a radius of 1m around the origin

        vel.z = 0; //Flatten vel onto the xy plane
        vel.Normalize();// make the length of vel 1;
        //normalizing a vector makes it's length 1m
        vel *= Random.Range(driftMinMax.x, driftMinMax.y);
        //Above sets the velocity length to something between the x and y
        //values for diftMinMax

        GetComponent<Rigidbody>().velocity = vel;

        //set the rotation of the GameOjbect to R:[0,0,0]
        transform.rotation = Quaternion.identity;
        //Quaternion.identity is equal to no rotation

        //set up the rotPerSecond for the Cube child using rotMinMax x & y
        rotPerSecond = new Vector3(Random.Range(rotMinMax.x, rotMinMax.y),
                                   Random.Range(rotMinMax.x, rotMinMax.y),
                                   Random.Range(rotMinMax.x, rotMinMax.y) );

        //checkOffScreen every 2 seconds
        InvokeRepeating("CheckOffscreen", 2f, 2f);

        birthTime = Time.time;
    }//end Awake

    void Update()
    {
        //manually rotate the cube child every update
        //multiplying it by Time.time causes the rotation to be time-based
        cube.transform.rotation = Quaternion.Euler(rotPerSecond * Time.time);

        //Fade out the PowerUp over time
        //Given the default values, a PowerUp will exist for 10 seconds
        //and then fade out over 4 seconds
        float u = (Time.time - (birthTime + lifeTime)) / fadeTime;
        //For lifeTime seconds, u will be <= 0. Then it will transition to 1 
        //over fadeTime seconds.

        //if u >= 1, destroy this PowerUp
        if(u >= 1)
        {
            Destroy(this.gameObject);
            return;
        }

        //use u to determine the alpha value of the Cube and Letter
        if(u > 0)
        {
            Color c = cube.GetComponent<Renderer>().material.color;
            c.a = 1f - u;
            cube.GetComponent<Renderer>().material.color = c;

            //Fade the letter too, but only half as much
            c = letter.color;
            c.a = 1f - (u * 0.5f);
            letter.color = c;
        }
    }//end update

    //This SetType() differs from those on Weapon and Projectile 
    public void SetType(WeaponType wt)
    {
        //grab the weapon definition from Main
        WeaponDefinition def = Main.GetWeaponDefinition(wt);

        //set the color of the Cube child
        cube.GetComponent<Renderer>().material.color = def.color;
        //letter.color = def.color; //we could colorize the letter too
        letter.text = def.letter; // set the letter that is shown
        type = wt; // actually set the type
    }

    public void AbsorbedBy(GameObject target)
    {
        //this function is called by the Hero class when a PowerUp is collected
        //we could tween into the target and shrink in size, but
        //for now, just destroy this gameObject
        Destroy(this.gameObject);
    }

    void CheckOffscreen()
    {
        //if the powerup has drifted entirely off screen
        if (Utils.ScreenBoundsCheck(cube.GetComponent<Collider>().bounds, BoundsTest.offscreen) != Vector3.zero)
        {
            //then destroy this gameObject
            Destroy(this.gameObject);
        }
    }
}
