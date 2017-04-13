using UnityEngine;
using UnityEngine.UI;//needed to access slider
using System.Collections;

public class KeepScore : MonoBehaviour {

	public Slider score;

	void Awake()
	{
		DontDestroyOnLoad (this);
		DontDestroyOnLoad (score);
	}
	//does it work this way?
}
