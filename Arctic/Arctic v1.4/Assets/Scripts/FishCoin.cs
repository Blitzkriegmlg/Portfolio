using UnityEngine;
using System.Collections;
using UnityEngine.UI;

public class FishCoin : MonoBehaviour {
    public GameObject scoreText;
	static public int score=0;
	void OnTriggerEnter2D(Collider2D c) {
		if (c.CompareTag("Player")) {
			score++;
            scoreText.GetComponent<Text>().text = "Score: " + score;
			Destroy(this.gameObject);
		}
	}
}
