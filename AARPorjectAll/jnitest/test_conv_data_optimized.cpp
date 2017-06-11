#include <stdio.h>
#include <stdlib.h>


float abs(float value){
	if(value>=0) return value;
	else return -1*value;
}

// function used in ultraphone to get only the necessary convolutions
float getSumAbsConInRange(float *source, int sourceSize, float *pulse, int pulseSize, int rangeStart, int rangeEnd){
	int rangeSize = rangeEnd - rangeStart;
	int rangeOffset = (int)((pulseSize-1)/2); // this is used to compensate the "same" option conv in matlab

	printf("rangeOffset = %d\n", rangeOffset);
	
	// 1. make some data format check
	if(rangeOffset > rangeStart){
		printf("[ERROR]: rangeOffset > rangeStart");
		return -1;
	}
	if(sourceSize < pulseSize){
		printf("[ERROR]: only support sourceSize >= pulseSize");
		return -1;
	}

	float sum = 0;
	for(int i = 0; i<rangeSize; i++){
		int pulseIdx = 0;
		float con = 0;
		for( int x = i+rangeStart-rangeOffset; x < sourceSize && pulseIdx < pulseSize; x++) {
			printf("x = %d, source now = %f, pulse now = %f\n", x, source[x], pulse[pulseIdx]);
			con += source[x]*pulse[pulseIdx];
			pulseIdx ++;
		}
		printf("con = %f\n", con);
		sum += abs(con);
	}
	return sum;
}

int main(){
	float source[] = {2,-1,-4,-4,3,2,-5,1,2,-4};
	float pulse[] = {-1,0,2,-4,6};
	int rangeStart = 3-1; // note this is one less than the matlab setting for compensating c++ 0 start index
	int rangeEnd = 5;
	float result = getSumAbsConInRange(source,10,pulse,5,rangeStart, rangeEnd);
	printf("result = %f\n", result);
	return 0;
}
