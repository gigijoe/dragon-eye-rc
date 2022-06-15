// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("dragon_eye_rc");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("dragon_eye_rc")
//      }
//    }

/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <string>
#include "utils/logging.h"
#include "audio/PlayerController.h"
#include <android/asset_manager_jni.h>

char* convertJString(JNIEnv* env, jstring str)
{
    if ( !str ) std::string();

    const jsize len = env->GetStringUTFLength(str);
    const char* strChars = env->GetStringUTFChars(str, (jboolean *)0);

    return const_cast<char *>(strChars);
}

std::unique_ptr<PlayerController> mController;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_startPlaying(JNIEnv *env, jobject thiz,
                                                                jobject jAsset_manager,
                                                                jstring file_name) {
    LOGD("%s:%d\n", __PRETTY_FUNCTION__ , __LINE__);
    AAssetManager *assetManager = AAssetManager_fromJava(env,jAsset_manager);

    mController=std::make_unique<PlayerController>(*assetManager);

    char* trackFileName = convertJString(env,file_name);

    mController->start(trackFileName);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_stopPlaying(JNIEnv *env, jobject thiz) {
    LOGD("%s:%d\n", __PRETTY_FUNCTION__ , __LINE__);
    if(mController)
        mController->stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_native_1setDefaultStreamValues(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jint default_sample_rate,
                                                                                  jint default_frames_per_burst) {
    LOGD("%s:%d\n", __PRETTY_FUNCTION__ , __LINE__);
    LOGD("Default sample rate is %d, default frames per burst is %d\n", (int32_t) default_sample_rate, (int32_t) default_frames_per_burst);
    oboe::DefaultStreamValues::SampleRate = (int32_t) default_sample_rate;
    oboe::DefaultStreamValues::FramesPerBurst = (int32_t) default_frames_per_burst;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_isPlaying(JNIEnv *env, jobject thiz) {
    if(mController)
        return mController->isPlaying();
    else
        return false;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_gtek_dragon_1eye_1rc_DragonEyeApplication_isLoading(JNIEnv *env, jobject thiz) {
    if(mController)
        return (mController->state() == PlayerControllerState::Loading);
    else
        return false;
}