// 2016/04/27: This is used to test squeeze detection
#include <stdio.h>      /* printf */
#include <math.h>       /* sqrt */


void estimateMeanAndStd(float *s, int len, float &mean, float &std){
	float sumAvg = 0;
	float squareSumAvg = 0;

	for(int i=0;i<len;i++){
		sumAvg += s[i]/(float)len;
		squareSumAvg += s[i]*s[i]/(float)len;
	}

	mean = sumAvg;
	std = sqrt(squareSumAvg - mean*mean);
}

void dump(float *s, int len, char *tag){
	printf("%s = [", tag);
	for(int i=0;i<len;i++){
		printf("%.4f,", s[i]);
	}	
	printf("];\n");
}

// squeeze setting
bool USE_TWO_END_CORRECT = true;
bool NEED_ABS = true; // is it necessary in the end
int PEAK_WIN = 8;
float PEAK_HARD_THRES_HIGH			= 0.3;
float PEAK_HARD_THRES_LOW			= 0.2;

// *** WARN: soft thres can only be used in TWO_END_CORRECT mode ->
// otherwise the mean will be too high ***
float PEAK_SORT_THRES_RATIO_HIGH	= 1.5; // multiple of std to achieve for peak;
float PEAK_SORT_THRES_RATIO_LOW		= 0.5; // multiple of std to achieve for peak;
int PEAK_LOW_WIDTH_MAX				= 8; // with constrain of the peak values over low thres
int PEAK_LOW_WIDTH_MIN				= 2;
int CHECK_PEAK_CNT					= 2;
int CHECK_PEAK_DIFF_RANGE_MIN		= 6-1; // maltab index starts from 1 -> -1 to correct 
int CHECK_PEAK_DIFF_RANGE_MAX		= 25-1;
int CHECK_PEAK_OFFSET_START_RANGE_MIN = 0;
int CHECK_PEAK_OFFSET_START_RANGE_MAX = 40;
float CHECK_PEAK_RATIO_MIN			= 0.4; // 0.5 means peak should be twice as the edge
int PEAK_WIN_SIDE					= (int)(PEAK_WIN/2);

// this is the length of pressure estimate input into squeeze detector
#define SQUEEZE_LEN_TO_CHECK (30)

float sRatioTo2Ends[SQUEEZE_LEN_TO_CHECK];
// this function return the check status and update the audio buffer for squeeze processing
int LibSqueezeDetect(float s[]){
	const int CHECK_STATUS_INIT = -1;
    const int CHECK_STATUS_PASS_PEAK_CNT = 0;
    const int CHECK_STATUS_PASS_PEAK_X_OFFSET_DIFF = 1;
    const int CHECK_STATUS_PASS_PEAK_X_OFFSET_START = 2;
    const int CHECK_STATIS_PASS_PEAK_RATIO_MIN = 3;
 
	int X_CNT = SQUEEZE_LEN_TO_CHECK;
        
    //----------------------------------------------------------------------
    // 1. convert to force ratio based on the first or two ends signals
    //----------------------------------------------------------------------
    for(int xNow = 0; xNow<X_CNT; xNow++){
        float disToStart = xNow;
        float disToEnd = X_CNT - (xNow+1) ;
        float sRef = (s[0]*disToEnd + s[X_CNT-1]*disToStart)/(disToStart+disToEnd);
        sRatioTo2Ends[xNow] = (s[xNow] - sRef)/sRef;
		if(NEED_ABS){
			sRatioTo2Ends[xNow] = fabs(sRatioTo2Ends[xNow]);
		}
	 }
    
	float *sCorrected = sRatioTo2Ends; // always assume it uses the 2 end normalziation
   
	//dump(sCorrected, X_CNT, (char*)"sCorrectedJNI");
 
    //----------------------------------------------------------------------
    // 2. get thre based on mean, std, and hard thres
	// NOTE: use HARD threshold now -> no need to estimate std
	//----------------------------------------------------------------------
    //float sCorrectedMean, sCorrectedStd;
    //estimateMeanAndStd(sCorrected, X_CNT, sCorrectedMean, sCorrectedStd);
	
    float PEAK_THRES_HIGH = PEAK_HARD_THRES_HIGH;
    float PEAK_THRES_LOW = PEAK_HARD_THRES_LOW;
    
    //assert(PEAK_THRES_HIGH>PEAK_THRES_LOW,'[ERROR]: PEAK_THRES_HIGH<PEAK_THRES_LOW');
    
    //----------------------------------------------------------------------
    // 3. find peaks based on my ad-hoc way
    //    : based on skip peak-width of data once the peak is found
    //----------------------------------------------------------------------
    float *sToFindPeak = sCorrected;


	int peakX[SQUEEZE_LEN_TO_CHECK];
    float peakY[SQUEEZE_LEN_TO_CHECK];
    int valleyXLeft[SQUEEZE_LEN_TO_CHECK];
    int valleyXRight[SQUEEZE_LEN_TO_CHECK];
    float peakR[SQUEEZE_LEN_TO_CHECK];
    int peakXIdx = 0;
    
    // new peak detection, considering the "concaved peak"
    int xNow = 0;
    while(xNow < X_CNT){
        // find the start of data bigger than high thres
        if(sToFindPeak[xNow] >= PEAK_THRES_HIGH){
            // search the end of left/right low peak reference
            int lowPeakLeftNow = xNow; 
			for(int xRef=xNow-1; xRef>=0; xRef++){
                if (sToFindPeak[xRef] > PEAK_THRES_LOW){
                    lowPeakLeftNow = xRef;
                } else {
                    break;
                }
            }

            int lowPeakRightNow = xNow;
            for(int xRef=xNow+1; xRef<X_CNT; xRef++){
                if (sToFindPeak[xRef] > PEAK_THRES_LOW){
                    lowPeakRightNow = xRef;
                } else {
                    break;
                }
            }

            int lowPeakWidth = lowPeakRightNow - lowPeakLeftNow + 1;
            int peakXNow = (int)round(((float)lowPeakRightNow + (float)lowPeakLeftNow)/2);
            
			bool isPeak = false;
            if(peakXNow-PEAK_WIN_SIDE>=0 && peakXNow+PEAK_WIN_SIDE<X_CNT && lowPeakWidth >= PEAK_LOW_WIDTH_MIN && lowPeakWidth <= PEAK_LOW_WIDTH_MAX){
                isPeak = true; // only consider peak with proper peak width and peakX
			}


			// update peak information
            if(isPeak){
                peakX[peakXIdx] = peakXNow;

				/*
                yInLowRange = sToFindPeak(lowPeakLeftNow:lowPeakRightNow);
                peakY(peakXIdx) = mean(yInLowRange(yInLowRange>PEAK_THRES_HIGH));
                peakYNow = peakY(peakXIdx);
				*/
				

				float peakYSum = 0, peakYNow;
				float sumCnt = 0;
				for(int i=lowPeakLeftNow; i<=lowPeakRightNow; i++){
					if(sToFindPeak[i]>PEAK_THRES_HIGH){ // only concern the peak over the "high" thres
						peakYSum += sToFindPeak[i];
						sumCnt ++;
					}
				}
				if(sumCnt >0){
					peakYNow = peakYSum/sumCnt;//use the mean of signals over peak as the peakY
				}
				peakY[peakXIdx] = peakYNow;
               
				// search the low value in two side as the vally value 
				/*
                [~,valleyXLeft(peakXIdx)] = min(sToFindPeak(peakXNow-PEAK_WIN_SIDE:peakXNow));
                valleyXLeft(peakXIdx) = valleyXLeft(peakXIdx) + peakXNow-PEAK_WIN_SIDE-1;
                [~,valleyXRight(peakXIdx)] = min(sToFindPeak(peakXNow:peakXNow+PEAK_WIN_SIDE));
                valleyXRight(peakXIdx) = valleyXRight(peakXIdx) + peakXNow - 1;
				*/
			
				float minValueLeft = sToFindPeak[peakXNow];	
				valleyXLeft[peakXIdx] = peakXIdx; // init by the peak value
				for(int i=peakXNow-PEAK_WIN_SIDE;i<=peakXNow;i++){
					if(sToFindPeak[i]<minValueLeft){ // search the min value
						valleyXLeft[peakXIdx] = i;
						minValueLeft = sToFindPeak[i];
					}
				}

				float minValueRight = sToFindPeak[peakXNow];
                valleyXRight[peakXIdx] = peakXIdx; // init by the peak value
				for(int i=peakXNow; i<=peakXNow+PEAK_WIN_SIDE;i++){
					if(sToFindPeak[i]<minValueRight){ // search the min value
						valleyXRight[peakXIdx] = i;
						minValueRight =  sToFindPeak[i];
					}
				}

                peakR[peakXIdx] = (sToFindPeak[valleyXLeft[peakXIdx]]+sToFindPeak[valleyXRight[peakXIdx]])/(peakYNow*2);
                peakXIdx = peakXIdx + 1; // update number of peak
            }

            // update the xNow after this peak detections
            xNow = lowPeakRightNow+1;
        } else {
            xNow = xNow+1;
        }
    }
    
    // resize peakX vector -> no need in c++
	int peakCnt = peakXIdx; // just need to remember the end of peaks
    
    //----------------------------------------------------------------------
    // 4. check peak statistics
    //    : need to pass each "test" before the next test
    //----------------------------------------------------------------------
    int checkStatus = CHECK_STATUS_INIT;
    
    // a. check peak cnt
    if(peakCnt == CHECK_PEAK_CNT){
        checkStatus = CHECK_STATUS_PASS_PEAK_CNT;
    }
    
    // b. check peak offset/diff
    if(checkStatus == CHECK_STATUS_PASS_PEAK_CNT){
		bool passAllPeakDiffTest = true;

		// NOTE: need at least 
		for(int peakIdxToCheck = 1; peakIdxToCheck<peakCnt; peakIdxToCheck++){
			int peakDiff = peakX[peakIdxToCheck]-peakX[peakIdxToCheck-1];
			if (peakDiff >= CHECK_PEAK_DIFF_RANGE_MIN && peakDiff <= CHECK_PEAK_DIFF_RANGE_MAX){
				// do nothing -> pass the test
			} else {
				passAllPeakDiffTest = false;
				break;
			}
		}

		if(passAllPeakDiffTest){
			checkStatus = CHECK_STATUS_PASS_PEAK_X_OFFSET_DIFF;
		}
    }
    
    // c. check peak offset/start and end
	// NOTE: this is not used anymore
    if(checkStatus == CHECK_STATUS_PASS_PEAK_X_OFFSET_DIFF){
        if (peakX[0] >= CHECK_PEAK_OFFSET_START_RANGE_MIN && peakX[0] <= CHECK_PEAK_OFFSET_START_RANGE_MAX){
            checkStatus = CHECK_STATUS_PASS_PEAK_X_OFFSET_START;
        }
    }
    
    // d. check peak ratios
    if(checkStatus == CHECK_STATUS_PASS_PEAK_X_OFFSET_START){
		bool passAllPeakRatioTest = true;
		for(int peakIdxToCheck = 0; peakIdxToCheck<peakCnt; peakIdxToCheck++){
            float peakRatio = peakR[peakIdxToCheck];
			if(peakRatio < CHECK_PEAK_RATIO_MIN){
				// do nothing -> pass the test
			} else {
				passAllPeakRatioTest = false;
				break;
			}
		}
        if(passAllPeakRatioTest){
            checkStatus = CHECK_STATIS_PASS_PEAK_RATIO_MIN;
        }
    }

	return checkStatus;
}

float test1[] = {1.664077E+01,1.715513E+01,1.488591E+01,1.130239E+01,9.527195E+00,9.419062E+00,1.218606E+01,1.379858E+01,1.367252E+01,1.412200E+01,1.531822E+01,1.464419E+01,1.471949E+01,1.562555E+01,5.412372E+00,7.671205E+00,5.962048E+00,7.192734E+00,1.131244E+01,1.126536E+01,1.157147E+01,1.296099E+01,1.354315E+01,1.347590E+01,1.421162E+01,1.429725E+01,1.465762E+01,1.517788E+01,1.463519E+01,1.467212E+01};

int main(){
	printf("main starts\n");
	int resultStatus = LibSqueezeDetect(test1);
	printf("resultStatus = %d\n",resultStatus);
}
