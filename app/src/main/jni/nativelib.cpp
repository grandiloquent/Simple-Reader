#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <thread>
#include "cameraEngine.h"
#include "CameraWrapper.h"
#include "java_interop.h"
#include "server.h"

cameraEngine *cameraEngine1;
CameraWrapper *wrapper;

void imageReader::imagePreview(jint *image, jint width, jint height) {
    wrapper->drawImage(image, width, height);
}

extern "C" JNIEXPORT void JNICALL
Java_psycho_euphoria_translator_MainActivity_openCamera(JNIEnv *env, jclass clazz) {
    //wrapper = new CameraWrapper(env, pInstance);
    uint32_t w = 480;
    uint32_t h = 640;
    DisplayDimension dimension = DisplayDimension(w, h);
    cameraEngine1 = new cameraEngine(&dimension, true);
}

extern "C" JNIEXPORT void JNICALL
Java_psycho_euphoria_translator_MainActivity_cameraPreview(JNIEnv *env, jobject thiz) {
    cameraEngine1->startPreview(true);

    std::thread prewiewHandler(&CameraWrapper::imageGeting, wrapper, cameraEngine1);
    prewiewHandler.detach();
}

extern "C" JNIEXPORT void JNICALL
Java_psycho_euphoria_translator_MainActivity_takePhoto(JNIEnv *env, jclass clazz) {
    std::thread photoHandler(&cameraEngine::onTakeImage, cameraEngine1);
    photoHandler.detach();
}

extern "C" JNIEXPORT void JNICALL
Java_psycho_euphoria_translator_MainActivity_deleteCamera(JNIEnv *env, jclass clazz) {
    if (cameraEngine1) {
        cameraEngine1->deleteCamera();
        cameraEngine1 = nullptr;
    }
}

extern "C" jstring
Java_psycho_euphoria_translator_MainActivity_startServer(JNIEnv *env, jclass obj, jobject context,
                                                  jobject assetManager,
                                                  jstring ip,
                                                  jint port) {
    const std::string host = jsonparse::jni::Convert<std::string>::from(env, ip);
    StartServer(env, assetManager, host, port);

    char msg[60] = "Hello ";
    jstring result;
    result = env->NewStringUTF(msg);
    return result;
}
