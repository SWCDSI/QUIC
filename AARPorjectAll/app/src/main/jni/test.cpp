/*
	2015/02/07: move all test functions to this file
*/

#include "common.h"

// this is a function to show log
extern "C" void Java_edu_umich_cse_audioanalysis_JniController_helloLog (JNIEnv *env, jobject obj, jstring logThis){
	const char * szLogThis = env->GetStringUTFChars(logThis, 0);
	debug("%s",szLogThis);

	env->ReleaseStringUTFChars(logThis, szLogThis);
} 

extern "C" void JNI_FUNC_NAME(testCon)(JNIEnv *env, jobject obj){
	debug("---testCon---");

	float a[] = {1,2,3,4,5};
	float b[] = {1,2,3,4};

	float *c = (float*)malloc(sizeof(float)*5);

	makeConvolveInDestSize(a, 5, b, 4, c, 5, true);

	debug("c = %f,%f,%f,%f,%f", c[0],c[1],c[2],c[3],c[4]);	
}


