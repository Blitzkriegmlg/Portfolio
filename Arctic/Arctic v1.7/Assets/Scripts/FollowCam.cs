using UnityEngine;
using System.Collections;

public class FollowCam : MonoBehaviour
{
    /*static public FollowCam S; //Singleton
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
	}*/

	/*----------------------------------------This version is for Camera Clamping---------------------------------------*/
	//This version helps limit the player's vision and allow them not to see beyond the world's bounds due to the 
	//main camera being restricted by these bounds.
	public Transform player;	//player for Camera to follow

	public Vector2 Margin;		//margin for camera size
	public Vector2 Smoothing;	//smooth Camera follow

	public BoxCollider2D CameraBounds;	//camera cannot exceed this area
	
	private Vector3 _min;		//the bottom left part of the CameraBounds
	private Vector3 _max;		//the top right part of the CameraBounds

	public bool IsFollowing { get; set;}

	public void Start(){
		_min = CameraBounds.bounds.min; //initiate _min
		_max = CameraBounds.bounds.max; //initiate _max
		IsFollowing = true; //Is the player being followed?
	}
	public void Update(){
		//take trans form position and determine if the main camera's bounds exceed the camera bounds we've set
		float x = transform.position.x; 
		float y = transform.position.y;
		if(IsFollowing){
			//if the main camera's x any bounds are greater
			//use linear interpolation to calculate the proper distance between camera border and player
			if(Mathf.Abs(x-player.position.x)>Margin.x){
				x = Mathf.Lerp(x,player.position.x,Smoothing.x*Time.deltaTime);
			}
			if(Mathf.Abs(y-player.position.y)>Margin.y){
				y = Mathf.Lerp(y,player.position.y,Smoothing.y*Time.deltaTime);
			}
			var cameraHalfWidth = GetComponent<Camera>().orthographicSize * ((float)Screen.width / Screen.height);

			x=Mathf.Clamp(x,_min.x+cameraHalfWidth, _max.x - cameraHalfWidth);
			y=Mathf.Clamp(y,_min.y+GetComponent<Camera>().orthographicSize, _max.y - GetComponent<Camera>().orthographicSize);

			//move to that position
			transform.position = new Vector3(x,y,transform.position.z);
		}
	}
}
