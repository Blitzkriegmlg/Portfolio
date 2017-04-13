using UnityEngine;
using System.Collections;

public class FishCoin : MonoBehaviour {
	static public int score=0;
	void OnTriggerEnter2D(Collider2D c) {
		if (c.CompareTag("Player")) {
			score++;
			//c.gameObject.GetComponent<Rigidbody2D>().AddForce(2f);
			Destroy(this.gameObject);
		}
	}
}
