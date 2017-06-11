// 2016/05/19: This class implement the auido player in ndk interface 
//             (help to reduce the delay of play/record audio)

#include "ndk_audio.h"
//#include "common.h"



// init the audio static buffer objects

// a mutext to guard against re-entrance to record & playback
// as well as make recording and playing back to be mutually exclusive
// this is to avoid crash at situations like:
//    recording is in session [not finished]
//    user presses record button and another recording coming in
// The action: when recording/playing back is not finished, ignore the new request
static pthread_mutex_t  audioEngineLock = PTHREAD_MUTEX_INITIALIZER;

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLVolumeItf bqPlayerVolume;
static SLmilliHertz bqPlayerSampleRate = 0;
static jint  bqPlayerBufSize = 0;

// recorder interfaces
static SLObjectItf recorderObject = NULL;
static SLRecordItf recorderRecord;
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;

// 5 seconds of recorded audio at 16 kHz mono, 16-bit signed little endian
// NOTE: this setting is based on s7
// WARN: this setting needs to fit other ndk setting!! (TODO: check everytime I modify the setting)
#define NDK_AUDIO_RECORD_CH 1
#define NDK_AUDIO_FS_IN_SEC 48000
#define NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT 480



#define NDK_AUDIO_PLAY_REPEAT_CNT_INF -2



// short
#define NDK_AUDIO_BYTE_PER_ELEMENT 2

// NOTE: this size is doubled -> since each element is 2 bytes
// so it SHOULD NOT used for count buffer elements
#define NDK_AUDIO_PLAY_SINGLE_BUFF_BYTE_SIZE (NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT*NDK_AUDIO_BYTE_PER_ELEMENT)
#define NDK_AUDIO_RECORD_SINGLE_BUFF_BYTE_SIZE (NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT*NDK_AUDIO_BYTE_PER_ELEMENT*NDK_AUDIO_RECORD_CH)

// this is the elements per channel in total
#define NDK_AUDIO_RECORD_BUFF_CNT 10000
// ensure the NDK_AUDIO_RECORD_BUFF_ELEMENT_CNT*(NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT/NDK_AUDIO_FS_IN_SEC) is larger than the sec you want to read!
#define NDK_AUDIO_RECORD_MAX_SIZE (NDK_AUDIO_RECORD_BUFF_CNT*NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT*NDK_AUDIO_RECORD_CH)

// pilot used for ndk audio
#define NDK_AUDIO_PILOT_USED (NDK_AUDIO_PILOT_080_VOL)
#define NDK_AUDIO_PILOT_ELEMENT_CNT 480
#define NDK_AUDIO_PILOT_REPEAT_CNT 8

// pulse used for ndk audio
#define NDK_AUDIO_PULSE_USED (NDK_AUDIO_PULSE_080_VOL)
#define NDK_AUDIO_PULSE_ELEMENT_CNT 2400
//#define NDK_AUDIO_PULSE_USED (NDK_AUDIO_TEST)
//#define NDK_AUDIO_PULSE_ELEMENT_CNT (48000)
#define NDK_AUDIO_PULSE_REPEAT_CNT -2

static short ndkAudioRecordBufferBase[NDK_AUDIO_RECORD_MAX_SIZE]; // this is the "VERY LARGE" buffer used to save all ndk audio data
static short *ndkAudioRecordBufferNextPtr = ndkAudioRecordBufferBase;
static unsigned ndkAudioRecordBufferEnd = 0;

// pointer and size of the next player buffer to enqueue, and number of remaining buffers
static short *ndkAudioPlayBufferStart; // this is the pointer to the buffer start
static int ndkAudioPlayBufferSize; // size of the buffer to be played
static int ndkAudioPlayBufferOffset; // current offset to play buffer, note this is 0 when the first callback is called
static int ndkAudioPlayRemainingRepeatCnt = 0; // set it to 1 for play once!!

// file saving variables
static int currentRecordSavedToFileIdx = 0;
static char* ndkLogFolderPath;

static int totalPlayCallbackCalledCnt = 0;
static int totalRecordCallbackCalledCnt = 0;

// internal state
#define NDK_AUDIO_PLAY_STATE_NOT_INIT -1
#define NDK_AUDIO_PLAY_STATE_PILOT 1
#define NDK_AUDIO_PLAY_STATE_PULSE 2
static int ndkAudioPlayState = NDK_AUDIO_PLAY_STATE_NOT_INIT;

#define NDK_AUDIO_PROCESS_STATE_INIT -1
#define NDK_AUDIO_PROCESS_STATE_SEARCH_PILOT 1
#define NDK_AUDIO_PROCESS_STATE_ESTIMATE_FORCE 2
static int ndkAudioProcessState = NDK_AUDIO_PROCESS_STATE_INIT;

// ForcePhone internal state
static bool isNdkAudioPlaying = false;
static bool isNdkAudioRecording = false;

// ForcePhone processing
#define MAX_BUFF_CNT_TO_SEARCH_PILOT 30
static int ndkAudioSensingPilotMaxIdxs[MAX_BUFF_CNT_TO_SEARCH_PILOT]; // assume 30 single buffer read can get the buffer
//static int ndkAudioProcessingState = 

//=================================================================================
//  Utility functions
//=================================================================================
// process signals -> This must be called "everytime" the record callback ends
void processAudioBuffIfNeed(int bufferBaseOffset){
	//if(ndkAudioSensingState == )
	if(ndkAudioProcessState == NDK_AUDIO_PROCESS_STATE_SEARCH_PILOT){

	} else {
		check(0==1,"Undefined ndkAudioProcessState = %d",ndkAudioProcessState);
	}
}

// this function is the ForcePhone-based init which enqueue the buffer before the player/record is triggered
void initAndEnqueueTheFirstAudioPlayBuff(){
	SLresult result;

	ndkAudioPlayState = NDK_AUDIO_PLAY_STATE_PILOT;
	ndkAudioPlayBufferStart = NDK_AUDIO_PILOT_USED; // this is the pointer to the buffer start
	ndkAudioPlayBufferSize = NDK_AUDIO_PILOT_ELEMENT_CNT; // size of the buffer to be played
	ndkAudioPlayBufferOffset = 0; // current offset to play buffer, note this is 0 when the first callback is called
	ndkAudioPlayRemainingRepeatCnt = NDK_AUDIO_PILOT_REPEAT_CNT; // set it to 1 for play once!!

	totalPlayCallbackCalledCnt = 0;

	// push the first play buffer
    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, ndkAudioPlayBufferStart, NDK_AUDIO_PLAY_SINGLE_BUFF_BYTE_SIZE);
    check(SL_RESULT_SUCCESS == result, "Fail to enqueu the first playBuffer, result = %d", result);
	(void)result;
}

void initAndEnqueueTheFirstRecordBuff(){
	SLresult result;
	
	ndkAudioProcessState = NDK_AUDIO_PROCESS_STATE_SEARCH_PILOT;
	ndkAudioRecordBufferNextPtr = &ndkAudioRecordBufferBase[0];
	ndkAudioRecordBufferEnd = 0;
	totalRecordCallbackCalledCnt = 0;
    
	// push the first buffer for init the recording
	result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, ndkAudioRecordBufferNextPtr, NDK_AUDIO_RECORD_SINGLE_BUFF_BYTE_SIZE);
    check(SL_RESULT_SUCCESS == result, "Fail to enqueue the first recordBuffer, result = %d", result);
    (void)result;
}

//=================================================================================
//  Callback functions (called by system buffer event like audio buffer is full)
//=================================================================================

// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	totalPlayCallbackCalledCnt ++;
    check(bq == bqPlayerBufferQueue, "Fail to check bq == bqPlayerBufferQueue");
    check(NULL == context, "Fail to check NULL == context");

    //debug("ndkAudioPlayBufferOffset = %d",ndkAudioPlayBufferOffset);

    // prepare the next buffer offset to play
	ndkAudioPlayBufferOffset += NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT;
	check(ndkAudioPlayBufferOffset <= ndkAudioPlayBufferSize, "ndkAudioPlayBufferOffset = %d overflow (ndkAudioPlayBufferSize = %d is not a multiple of NDK_AUDIO_PLAY_SINGLE_BUFF_BYTE_SIZE = %d?)", ndkAudioPlayBufferOffset, ndkAudioPlayBufferSize, NDK_AUDIO_PLAY_SINGLE_BUFF_BYTE_SIZE);
	if(ndkAudioPlayBufferOffset == ndkAudioPlayBufferSize){
		// the whole buffer is played -> prepare the next play
		ndkAudioPlayBufferOffset = 0;
		if(ndkAudioPlayRemainingRepeatCnt!=NDK_AUDIO_PLAY_REPEAT_CNT_INF){
			ndkAudioPlayRemainingRepeatCnt --;
		}
	}

	// modify play state if need
	if(ndkAudioPlayState == NDK_AUDIO_PLAY_STATE_PILOT && ndkAudioPlayRemainingRepeatCnt == 0){
		debug("changed to NDK_AUDIO_PLAY_STATE_PULSE");
		ndkAudioPlayState = NDK_AUDIO_PLAY_STATE_PULSE;
		ndkAudioPlayBufferStart = NDK_AUDIO_PULSE_USED; // this is the pointer to the buffer start
		ndkAudioPlayBufferSize = NDK_AUDIO_PULSE_ELEMENT_CNT; // size of the buffer to be played
		ndkAudioPlayBufferOffset = 0; // current offset to play buffer, note this is 0 when the first callback is called
		ndkAudioPlayRemainingRepeatCnt = NDK_AUDIO_PULSE_REPEAT_CNT; // set it to 1 for play once!!
	}

	// play the sound with the correct offset/ptr
	if(ndkAudioPlayRemainingRepeatCnt == NDK_AUDIO_PLAY_REPEAT_CNT_INF || ndkAudioPlayRemainingRepeatCnt > 0){
		SLresult result;
		result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, ndkAudioPlayBufferStart+ndkAudioPlayBufferOffset, NDK_AUDIO_PLAY_SINGLE_BUFF_BYTE_SIZE);
    	check(SL_RESULT_SUCCESS == result, "Fail to enqueu buffer in callback, result = %d", result);
		(void)result;
	} else {
		check(0==1, "Nothing to paly, must be wrong setting of ndkAudioPlayRemainingRepeatCnt = %d", ndkAudioPlayRemainingRepeatCnt);
	}
}

// this callback handler is called every time a buffer finishes recording
void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	totalRecordCallbackCalledCnt ++;
    check(bq == recorderBufferQueue, "Fail to check bq == recorderBufferQueue");
    check(NULL == context, "Fail to check NULL == context");
    
    SLresult result;
    // enqueu the next buff for audio recording
    if(ndkAudioRecordBufferEnd+NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT < NDK_AUDIO_RECORD_MAX_SIZE){ // has free buffer
    	// update buffer status
    	ndkAudioRecordBufferEnd += NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT;
    	ndkAudioRecordBufferNextPtr += NDK_AUDIO_SINGLE_BUFF_ELEMENT_CNT;

    	// enqueue the buffer
    	result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, ndkAudioRecordBufferNextPtr, NDK_AUDIO_RECORD_SINGLE_BUFF_BYTE_SIZE);
    	check(SL_RESULT_SUCCESS == result, "Fail to enqueue the first recordBuffer, result = %d", result);
    	(void)result;
    } else {
    	check(0==1, "No free buffer to record (size of buffer = %d too small?)", NDK_AUDIO_RECORD_MAX_SIZE);
    }
}

//=================================================================================
//  JNI interface functions
//=================================================================================
// just test if the debug mode of ndk-build is set
JNI_FUNC_HEAD void JNI_FUNC_NAME(testAssert)(JNI_FUNC_NO_PARAM){
	check(1==0, "1 is not 0, dumb %d", 5566);
}

// just test the debug method
JNI_FUNC_HEAD void JNI_FUNC_NAME(debugTestInNdkAudio)(JNI_FUNC_NO_PARAM){
	warn("I am Fuck ndk debug message from debugTestInNdkAudio: 5544");
}

// just for debug
JNI_FUNC_HEAD jint JNI_FUNC_NAME(getTotalPlayCallbackCalledCnt)(JNI_FUNC_NO_PARAM){
	return totalPlayCallbackCalledCnt;
}

// just for debug
JNI_FUNC_HEAD jint JNI_FUNC_NAME(getTotalRecordCallbackCalledCnt)(JNI_FUNC_NO_PARAM){
	return totalRecordCallbackCalledCnt;
}

// init the log path
JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioInit)(JNI_FUNC_PARAM jstring logFolderPathIn){
	debug("--- ndkAudioInit: start ---");
	
    const char *logFolderPath = env->GetStringUTFChars(logFolderPathIn, 0);
    ndkLogFolderPath = (char*)malloc(sizeof(char)*strlen(logFolderPath));
    strcpy(ndkLogFolderPath, logFolderPath);
	debug("ndkLogFolderPath = %s",ndkLogFolderPath);
	// release resources
    env->ReleaseStringUTFChars(logFolderPathIn, logFolderPath);
}

// create audio engine
JNI_FUNC_HEAD void JNI_FUNC_NAME(createNdkAudioEngine)(JNI_FUNC_NO_PARAM){
	debug("--- createNdkAudioEngine: start ---");

	SLresult result;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    check(SL_RESULT_SUCCESS == result, "Fail to create engineObject");
    (void)result;

    // realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    check(SL_RESULT_SUCCESS == result, "Fail to realize engineObject");
    (void)result;

    // get the engine interface, which is needed in order to create other objects
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    check(SL_RESULT_SUCCESS == result, "Fail to get engineObject interface");
    (void)result;

    // create output mix, with environmental reverb specified as a non-required interface
    // TODO: remove other interface that might cause long delay
    const SLInterfaceID ids[1] = {SL_IID_VOLUME};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, ids, req);
    check(SL_RESULT_SUCCESS == result, "Fail to create outputMixObject");
    (void)result;

    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    check(SL_RESULT_SUCCESS == result, "Fail to realize outputMixObject");
    (void)result;

    debug("--- createNdkAudioEngine: end ---");
}

JNI_FUNC_HEAD void JNI_FUNC_NAME(createNdkAudioPlayer)(JNI_FUNC_NO_PARAM){
	debug("--- createNdkAudioPlayer: start ---");
	SLresult result;

	// NOTE: the sample rate in ndk player is 1000 times of the original one
	bqPlayerSampleRate = NDK_AUDIO_FS_IN_SEC*1000; // ms

    // configure audio source
    // ref: http://stackoverflow.com/questions/21994361/what-is-sldatalocator-androidsimplebufferqueue-android-4-3
    // TODO: change the sample rate to 48000 or higher
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_8, SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    //SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_8, SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, SL_BYTEORDER_LITTLEENDIAN};

    /*
     * Enable Fast Audio when possible:  once we set the same rate to be the native, fast audio path
     * will be triggered
     */
    if(bqPlayerSampleRate) {
        format_pcm.samplesPerSec = bqPlayerSampleRate;       //sample rate in mili second
    }
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

	/*
     * create audio player:
     *     fast audio does not support when SL_IID_EFFECTSEND is required, skip it
     *     for fast audio case
     */
    const SLInterfaceID ids[2] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk, 2, ids, req);
    check(SL_RESULT_SUCCESS == result, "Fail to create bqPlayerObject, result = %d",result);
    (void)result;

    // realize the player
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    check(SL_RESULT_SUCCESS == result, "Fail to realize bqPlayerObject");
    (void)result;

    // get the play interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
    check(SL_RESULT_SUCCESS == result, "Fail to get bqPlayerPlay interface");
    (void)result;

    // get the buffer queue interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
            &bqPlayerBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to get bqPlayerBufferQueue interface");
    (void)result;

    // register callback on the buffer queue
    result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, NULL);
    check(SL_RESULT_SUCCESS == result, "Fail to set callback for bqPlayerBufferQueue");
    (void)result;


    // init setting and enqueu the first buffer to save trigger time
    initAndEnqueueTheFirstAudioPlayBuff();
    

    debug("--- createNdkAudioPlayer: end ---");
}

/*
// NOTE: this play function must be called after the audio player is created
JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioPlayerStartPlay)(JNI_FUNC_NO_PARAM){
	debug("ndkAudioPlayerStartPlay: start");
	SLresult result;

	// set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    check(SL_RESULT_SUCCESS == result, "Fail to set bqPlayerPlay as SL_PLAYSTATE_PLAYING");
    (void)result;

    debug("ndkAudioPlayerStartPlay: end");
}

JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioPlayerStopPlay)(JNI_FUNC_NO_PARAM){
	debug("ndkAudioPlayerStopPlay: start");
	// TOOD: also need to init the buffer index
	SLresult result;;

    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_STOPPED);
    check(SL_RESULT_SUCCESS == result, "Fail to set bqPlayerPlay as SL_PLAYSTATE_STOP");
    (void)result;

    result = (*bqPlayerBufferQueue)->Clear(bqPlayerBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to clear ");
    (void)result;

    // remember to enqueue the next buffer
    if (nextCount == PLAY_CNT_INF || nextSize > 0) {
        // here we only enqueue one buffer because it is a long clip,
        // but for streaming playback we would typically enqueue at least 2 buffers to start
        SLresult result;
        result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, nextBuffer, nextSize);
        check(SL_RESULT_SUCCESS == result, "Fail to enqueue nextBuffer");
    }

	debug("ndkAudioPlayerStopPlay: end");
}
*/

JNI_FUNC_HEAD void JNI_FUNC_NAME(createNdkAudioRecorder)(JNI_FUNC_NO_PARAM){
	debug("--- createNdkAudioRecorder: start ---");
	SLresult result;

	// configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
            SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    int RECORD_CH = 1; // TODO: make it to stereo recording
    // TOP microphone
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, RECORD_CH, SL_SAMPLINGRATE_48, SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    // NOTE: if you want to change microphone -> you need to also change the one in stop function
    // bottom microphone
    //SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, RECORD_CH, SL_SAMPLINGRATE_48, SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    // create audio recorder
    // (requires the RECORD_AUDIO permission)
    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject, &audioSrc,
            &audioSnk, 1, id, req);
    check(SL_RESULT_SUCCESS == result, "Fail to create recorderObject, result = %d", result);
    (void)result;

    // realize the audio recorder
    result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
    check(SL_RESULT_SUCCESS == result, "Fail to realize recorderObject, result = %d", result);
    (void)result;

    // get the record interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recorderRecord);
    check(SL_RESULT_SUCCESS == result, "Fail to get recorderRecord interface");
    (void)result;

    // get the buffer queue interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,&recorderBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to get recorderBufferQueue interface");
    (void)result;

    // register callback on the buffer queue
    result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, bqRecorderCallback, NULL);
    check(SL_RESULT_SUCCESS == result, "Fail to register bqRecorderCallback");
    (void)result;

    // init and enqueue the first buffer to save triggering time
    initAndEnqueueTheFirstRecordBuff();

	debug("--- createNdkAudioRecorder: end ---");
}

// set the recording state for the audio recorder
/*
JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioRecrderStartRecord)(JNI_FUNC_NO_PARAM){
	debug("ndkAudioRecrderStartRecord: start");
    SLresult result;

    // TODO: think my way to make it
    // in case already recording, stop recording and clear buffer queue
    //result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
    //assert(SL_RESULT_SUCCESS == result);
    //(void)result;
    //result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
    //assert(SL_RESULT_SUCCESS == result);
    //(void)result;

    // the buffer is not valid for playback yet
    recorderSize = 0;

    // enqueue an empty buffer to be filled by the recorder
    // (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer, RECORDER_FRAMES * sizeof(short));
    // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
    // which for this code example would indicate a programming error
    check(SL_RESULT_SUCCESS == result, "Fail to enqueue recordBuffer, result = %d", result);
    (void)result;

    // start recording
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
    check(SL_RESULT_SUCCESS == result, "Fail to set recorderRecord to SL_RECORDSTATE_RECORDING");
    (void)result;

    debug("ndkAudioRecrderStartRecord: end");
}
*/

// this function trigger both player and recorder simulteneously
// try to minimize the delay before the triggering
JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioForcePhoneStartSensing)(JNI_FUNC_NO_PARAM){
	debug("--- ndkAudioForcePhoneStartSensing: start ---");
	check(!isNdkAudioPlaying && !isNdkAudioRecording, "Fail to start sensing when audio is playing = %d or recording = %d (forget to stop sensing in Java?)", isNdkAudioPlaying, isNdkAudioPlaying);
	
	SLresult result;

	// init internal states and buffers
	// TODO: move this buff init to stop function and engiene init -> save delay if need
	// initAndEnqueueTheFirstRecordPlayerBuff();
	// buffer should be enqueued before this function is called

    // start recording
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
    check(SL_RESULT_SUCCESS == result, "Fail to set recorderRecord to SL_RECORDSTATE_RECORDING");
    (void)result;
    isNdkAudioRecording = true;

	// set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    check(SL_RESULT_SUCCESS == result, "Fail to set bqPlayerPlay to SL_PLAYSTATE_PLAYING");
    (void)result;
    isNdkAudioPlaying = true;
	debug("--- ndkAudioForcePhoneStartSensing: end ---");
}

// this need to be called everytime the play ends
JNI_FUNC_HEAD void JNI_FUNC_NAME(ndkAudioForcePhoneStopSensing)(JNI_FUNC_NO_PARAM){
	debug("--- ndkAudioForcePhoneStopSensing: start ---");
	check(isNdkAudioPlaying && isNdkAudioRecording, "Fail to stop sensing when audio is not playing = %d or recording = %d (forget to stop sensing in Java?)", isNdkAudioPlaying, isNdkAudioPlaying);

	// write current buffer to file
	char recordFilePath[1000];
	sprintf(recordFilePath,"%sndkAudio%03d.dat", ndkLogFolderPath, currentRecordSavedToFileIdx);


	// show some statistic for debug
    debug("totoal play/record callback called cnt = (%d,%d)", totalPlayCallbackCalledCnt, totalRecordCallbackCalledCnt);
    debug("record file is going to be saved to %s", recordFilePath);
    debug("record end = %d", ndkAudioRecordBufferEnd);

    FILE* file = fopen( recordFilePath, "wb" );
	fwrite( ndkAudioRecordBufferBase, 1, ndkAudioRecordBufferEnd*NDK_AUDIO_BYTE_PER_ELEMENT, file );
	fclose(file);


	SLresult result;
    // stop ndk playing
	result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_STOPPED);
    check(SL_RESULT_SUCCESS == result, "Fail to set bqPlayerPlay to SL_PLAYSTATE_STOP");
    (void)result;
    isNdkAudioPlaying = false;

    result = (*bqPlayerBufferQueue)->Clear(bqPlayerBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to clear bqPlayerBufferQueue");
    (void)result;

    // init and enqueue the new buffer the save trigger time
    initAndEnqueueTheFirstAudioPlayBuff();

	// stop ndk recording
	result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
    check(SL_RESULT_SUCCESS == result, "Fail to set recorderRecord to SL_RECORDSTATE_STOPPED");
    isNdkAudioRecording = false;

	result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to clear recorderBufferQueue");
    (void)result;


// *** start of stupid fix -> renew another recorder... ***    
    warn("*** start of stupid fix ***");
	(*recorderObject)->Destroy(recorderObject);

    // configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
            SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    int RECORD_CH = 1; // TODO: make it to stereo recording
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, RECORD_CH, SL_SAMPLINGRATE_48, SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16, SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    // create audio recorder
    // (requires the RECORD_AUDIO permission)
    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject, &audioSrc,
            &audioSnk, 1, id, req);
    check(SL_RESULT_SUCCESS == result, "Fail to create recorderObject, result = %d", result);
    (void)result;

    // realize the audio recorder
    result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
    check(SL_RESULT_SUCCESS == result, "Fail to realize recorderObject, result = %d", result);
    (void)result;

    // get the record interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recorderRecord);
    check(SL_RESULT_SUCCESS == result, "Fail to get recorderRecord interface");
    (void)result;

	// get the buffer queue interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,&recorderBufferQueue);
    check(SL_RESULT_SUCCESS == result, "Fail to get recorderBufferQueue interface");
    (void)result;

    // register callback on the buffer queue
    result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, bqRecorderCallback, NULL);
    check(SL_RESULT_SUCCESS == result, "Fail to register bqRecorderCallback");
    (void)result;
	warn("*** end of stupid fix ***");
// *** end of stupid fix ***    

    // init and enqueue the new buffer the save trigger time
    initAndEnqueueTheFirstRecordBuff();


    // update some statistic variables
    currentRecordSavedToFileIdx ++;

	debug("--- ndkAudioForcePhoneStopSensing: end ---");
}
