using UnityEngine;
using System.Collections;

public class FollowCam : MonoBehaviour
{
    static public FollowCam S; //Singleton
    public GameObject focus; //the GameObject for the camera to follow
    public float camZ; //The desired Z position of the camera.
    //despite being in 2D, the camera's Z position still matters

    private Vector3 destination;

    // Use this for initialization
    void Awake()
    {
        S = this;
        camZ = this.transform.position.z;
	}
	
	// Update is called once per frame
	void Update ()
    {
        if (focus == null) return; //Just in case a focus is not set

        //Get the position of focus
        destination = focus.transform.position;

        //keep the original z position
        destination.z = camZ;

        //set the position of the camera
        transform.position = destination;
	}
}
