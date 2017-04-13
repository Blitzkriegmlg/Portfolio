using UnityEngine;
using System.Collections;

public class ObstacleMove : MonoBehaviour {

	public float moveSpeed = 1;
	public Vector3 homePoint;

	// Use this for initialization
	void Start () {
	
	}
	
	// Update is called once per frame
	void Update () {
		transform.Translate (Vector3.left * Time.deltaTime * moveSpeed);
	}
}
