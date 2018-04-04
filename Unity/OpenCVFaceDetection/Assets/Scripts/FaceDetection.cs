using System;
using UnityEngine;

public class FaceDetection : MonoBehaviour
{

    private Texture2D nativeTexture;

    private AndroidJavaObject context;


    void CreateTexture()
    {
#if UNITY_ANDROID
        AndroidJNI.AttachCurrentThread();
        // get the current activity from unity
        AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        this.context = jc.GetStatic<AndroidJavaObject>("currentActivity");

        Int32 texPtr = this.context.Call<Int32>("createTexture");
        Debug.Log("texture pointer == " + texPtr);
        nativeTexture = Texture2D.CreateExternalTexture(100, 100, TextureFormat.ARGB32, false, false, (IntPtr)texPtr);
        nativeTexture.UpdateExternalTexture(nativeTexture.GetNativeTexturePtr());
        GetComponent<Renderer>().material.mainTexture = nativeTexture;

        this.context.Call("setTextureId", texPtr);
#endif
    }

    void UpdateTexture()
    {
#if UNITY_ANDROID
        this.context.Call("updateTexture");
#endif
    }

    // Use this for initialization
    void Start()
    {
        AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
        this.context = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");

        this.CreateTexture();
    }

    // Update is called once per frame
    void Update()
    {
        this.UpdateTexture();
    }
}
