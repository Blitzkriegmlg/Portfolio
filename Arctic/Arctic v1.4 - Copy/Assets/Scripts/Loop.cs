using UnityEngine;
using System.Collections;

public class Loop : MonoBehaviour {

	public GameObject loopCenter;
	public float angle = -200.0f; // Degree per time unit
	public Vector3 axis = Vector3.forward; // Rotation axis

	void FixedUpdate(){
		loopCenter.GetComponent<Transform>().Rotate(-axis, Time.deltaTime * angle);//rotating center
	}

	void OnTriggerEnter2D(Collider2D c) {
		GameObject.FindGameObjectWithTag ("LetGo").GetComponent<Collider2D> ().enabled = false;//disable let go first
		if (c.CompareTag ("Player")) {//if playercomes close
			c.transform.parent = loopCenter.transform;//make penguin a child of the loop center
		}
		Delay();
		GameObject.FindGameObjectWithTag ("LetGo").GetComponent<Collider2D> ().enabled = true;// let go of penguin
	}
	IEnumerator Delay(){
		yield return new WaitForSeconds(5f);//wait 5 seconds
	}
}
