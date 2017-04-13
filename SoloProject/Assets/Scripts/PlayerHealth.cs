using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using UnityStandardAssets.Characters.FirstPerson;

public class PlayerHealth : MonoBehaviour
{
    public int startingHealth = 100;                            // The amount of health the player starts the game with.
    public int currentHealth;                                   // The current health the player has.
    public Slider healthSlider;                                 // Reference to the UI's health bar.
    public Slider scoreSlider;                                  //hold the score for the match.
    public Image damageImage;                                   // Reference to an image to flash on the screen on being hurt.
    public AudioClip deathClip;                                 // The audio clip to play when the player dies.
    public float flashSpeed = 5f;                               // The speed the damageImage will fade at.
    public Color flashColour = new Color(1f, 0f, 0f, 0.1f);     // The colour the damageImage is set to, to flash.


    //Animator anim;                                              // Reference to the Animator component.
    AudioSource playerAudio;                                    // Reference to the AudioSource component.
    FirstPersonController playerMovement;                              // Reference to the player's movement.
    PlayerShoot playerShooting;                              // Reference to the PlayerShooting script.
    bool isDead;                                                // Whether the player is dead.
    bool damaged;                                               // True when the player gets damaged.


    void Awake()
    {
        // Setting up the references.
        //anim = GetComponent<Animator>();
        playerAudio = GetComponent<AudioSource>();
        playerMovement = GetComponent<FirstPersonController>();
        playerShooting = GetComponentInChildren<PlayerShoot>();

        // Set the initial health of the player.
        currentHealth = startingHealth;
        isDead = false;
    }


    void Update()
    {
        // If the player has just been damaged...
        if (damaged)
        {
            // ... set the colour of the damageImage to the flash colour.
            damageImage.color = flashColour;
        }
        // Otherwise...
        else
        {
            // ... transition the colour back to clear.
            damageImage.color = Color.Lerp(damageImage.color, Color.clear, flashSpeed * Time.deltaTime);
        }

        //if the character falls off
        if (transform.position.y <= -5)
        {
            TakeDamage(100);
        }

        // Reset the damaged flag.
        damaged = false;
    }


    public void TakeDamage(int amount)
    {
        // Set the damaged flag so the screen will flash.
        damaged = true;

        // Reduce the current health by the damage amount.
        currentHealth -= amount;

        // Set the health bar's value to the current health.
        healthSlider.value = currentHealth;

        // Play the hurt sound effect.
        playerAudio.Play();

        // If the player has lost all it's health and the death flag hasn't been set yet...
        if (currentHealth <= 0 && !isDead)
        {
            // ... it should die.
            Death();
        }
    }


    void Death()
    {
        // Set the death flag so this function won't be called again.
        isDead = true;

        scoreSlider.value += 1;


        Respawn();
        // Tell the animator that the player is dead.
        //anim.SetTrigger("Die");

        // Set the audiosource to play the death clip and play it (this will stop the hurt sound from playing).
        //playerAudio.clip = deathClip;
        //playerAudio.Play();



        
        //Application.LoadLevel(0);
    }

    void Respawn()
    {
        
        //refill health
        currentHealth = startingHealth;
        healthSlider.value = currentHealth;
        isDead = false;

        //random respawns
        float random = Random.value;

        Vector3 newPos;


        if (random < .2)
        {
            //this.transform.position.Set(-15.04f, -2.656f, -13.68f);
            newPos = new Vector3(-15.04f, -2.656f, -13.68f);
            transform.position = newPos;
        }

        if (random >= .2 && random < .4)
        {
            //this.transform.position.Set(0.79f, -2.66f, -13.68f);
            newPos = new Vector3(0.79f, -2.66f, -13.68f);
            transform.position = newPos;
        }

        if (random >= .4 && random < .6)
        {
            //this.transform.position.Set(13.4f, -2.71f, 15.93f);
            newPos = new Vector3(13.4f, -2.71f, 15.93f);
            transform.position = newPos;
        }

        if (random >= .6 && random < .8)
        {
            //this.transform.position.Set(1.33f, .62f, 15.93f);
            newPos = new Vector3(1.33f, .62f, 15.93f);
            transform.position = newPos;
        }

        if (random >= .8 && random < 1)
        {
            //this.transform.position.Set(-14.59f, .66f, 3.61f);
            newPos = new Vector3(-14.59f, .66f, 3.61f);
            transform.position = newPos;
        }

    }
}