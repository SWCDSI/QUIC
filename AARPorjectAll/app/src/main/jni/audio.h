/*
	2015/11/11: this file includes the define of audio setting
	2016/05/07: make it as a shared file with ios xcode -> should only have the shared preference in here!
*/

//=========================================================================================================
//         !!! NOTE !!! It is a shared preference -> modify it will affect both iOS and Android code
//=========================================================================================================


int AUDIO_SAMPLE_TO_FIND_PILOT = 35000; // note4 setting
// Default setting
int AUDIO_CH_IDX_TO_FIND_PILOT = 2-1; // *** WARN: c++ is counted from 0 -> need to minus 1
// TOOD: make it changed based on the device setting (fine-tuned)
int AUDIO_PILOT_LEN = 500;
int AUDIO_PILOT_SEARCH_PEAK_WINDOW = 30;
int AUDIO_PILOT_SEARCH_THRES_FACTOR = 14;
int AUDIO_PILOT_REPEAT_CNT = 10;
int AUDIO_PILOT_REPEAT_DIFF = 1000;
int AUDIO_PILOT_END_OFFSET = 40960;
int AUDIO_SINGLE_REPEAT_LEN = 2400;
int AUDIO_PULSE_USED_LEN = 601; // NOTE: only used the middle part of pulse to detect
int AUDIO_PRE_FILTER_LEN = 15; // legnth of filter a/b is the order + 1



// NOTE: AUDIO_PULSE_USED_TO_FILTER needs to be set as reverse of AUDIO_PULSE_USE, the latter is used for optimization
float AUDIO_PILOT_TO_FILTER[] = {1.000000E+00,-3.835315E-01,-7.045051E-01,9.270101E-01,-1.468962E-02,-9.148531E-01,7.300882E-01,3.407462E-01,-9.982741E-01,4.502702E-01,6.392978E-01,-9.606107E-01,1.318265E-01,8.536392E-01,-8.222496E-01,-1.850462E-01,9.725045E-01,-6.115672E-01,-4.687920E-01,9.981246E-01,-3.590536E-01,-6.984291E-01,9.424136E-01,-9.285990E-02,-8.633903E-01,8.229208E-01,1.639954E-01,-9.619838E-01,6.592472E-01,3.948885E-01,-9.991628E-01,4.702975E-01,5.896513E-01,-9.841556E-01,2.724754E-01,7.438290E-01,-9.283325E-01,7.874676E-02,8.575094E-01,-8.435089E-01,-1.016061E-01,9.340216E-01,-7.407529E-01,-2.627444E-01,9.787112E-01,-6.296669E-01,-4.017460E-01,9.979101E-01,-5.180629E-01,-5.179507E-01,9.981487E-01,-4.119303E-01,-6.122933E-01,9.856093E-01,-3.155959E-01,-6.866973E-01,9.657899E-01,-2.319912E-01,-7.435659E-01,9.433312E-01,-1.629602E-01,-7.853819E-01,9.219607E-01,-1.095622E-01,-8.144099E-01,9.045074E-01,-7.233820E-02,-8.324884E-01,8.929514E-01,-5.152385E-02,-8.408923E-01,8.884800E-01,-4.720078E-02,-8.402529E-01,8.915299E-01,-5.938144E-02,-8.305211E-01,9.018046E-01,-8.802680E-02,-8.109707E-01,9.182623E-01,-1.329965E-01,-7.802401E-01,9.390780E-01,-1.939324E-01,-7.364202E-01,9.615888E-01,-2.700768E-01,-6.772003E-01,9.822410E-01,-3.600327E-01,-6.000887E-01,9.965662E-01,-4.614771E-01,-5.027237E-01,9.992208E-01,-5.708514E-01,-3.832892E-01,9.841324E-01,-6.830656E-01,-2.410394E-01,9.448017E-01,-7.912727E-01,-7.691606E-02,8.748062E-01,-8.867887E-01,1.057806E-01,7.685398E-01,-9.592507E-01,3.007475E-01,6.221998E-01,-9.971177E-01,4.981815E-01,4.349807E-01,-9.886063E-01,6.845968E-01,2.103743E-01,-9.231248E-01,8.431565E-01,-4.261475E-02,-7.931936E-01,9.547239E-01,-3.086178E-01,-5.967260E-01,9.998013E-01,-5.657791E-01,-3.393894E-01,9.614086E-01,-7.867602E-01,-3.658604E-02,8.287637E-01,-9.412233E-01,2.855744E-01,6.013470E-01,-9.999730E-01,5.910275E-01,2.926058E-01,-9.406014E-01,8.368945E-01,-6.775881E-02,-7.539978E-01,9.793789E-01,-4.341538E-01,-4.505044E-01,9.822164E-01,-7.492396E-01,-6.396337E-02,8.266291E-01,-9.524731E-01,3.483803E-01,5.207529E-01,-9.926492E-01,7.119131E-01,1.056502E-01,-8.425918E-01,9.474349E-01,-3.453048E-01,-5.125550E-01,9.896852E-01,-7.362427E-01,-5.767926E-02,8.081990E-01,-9.684578E-01,4.252713E-01,4.246776E-01,-9.673702E-01,8.150184E-01,-8.031572E-02,-7.104377E-01,9.961296E-01,-5.777235E-01,-2.465092E-01,8.966986E-01,-9.199676E-01,3.036232E-01,5.219840E-01,-9.862885E-01,7.723860E-01,-3.042489E-02,-7.310734E-01,9.952424E-01,-5.863624E-01,-2.165250E-01,8.729571E-01,-9.455300E-01,3.893382E-01,4.237274E-01,-9.560410E-01,8.596611E-01,-2.012612E-01,-5.871059E-01,9.933914E-01,-7.574337E-01,3.475094E-02,7.089593E-01,-9.992717E-01,6.545002E-01,1.035631E-01,-7.951067E-01,9.869091E-01,-5.622049E-01,-2.112718E-01,8.526128E-01,-9.673037E-01,4.881405E-01,2.884642E-01,-8.881789E-01,9.487694E-01,-4.369874E-01,-3.363032E-01,9.071191E-01,-9.368955E-01,4.113326E-01,3.559913E-01,-9.127756E-01,9.346759E-01,-4.122888E-01,-3.481344E-01,9.062339E-01,-9.426327E-01,4.398167E-01,3.124828E-01,-8.862425E-01,9.588421E-01,-4.927127E-01,-2.480343E-01,8.493061E-01,-9.788456E-01,5.682640E-01,1.535060E-01,-7.899878E-01,9.955070E-01,-6.616109E-01,-2.819610E-02,7.015202E-01,-9.989525E-01,7.649191E-01,-1.267541E-01,-5.768667E-01,9.768136E-01,-8.665496E-01,3.067458E-01,4.103760E-01,-9.150648E-01,9.505364E-01,-5.019298E-01,-2.001048E-01,7.997760E-01,-9.968219E-01,6.956076E-01,-4.929693E-02,-6.200411E-01,9.828026E-01,-8.634564E-01,3.235501E-01,3.721163E-01,-8.867280E-01,9.744546E-01,-5.964102E-01,-6.435604E-02,6.932481E-01,-9.944862E-01,8.293504E-01,-2.781496E-01,-4.007849E-01,8.933643E-01,-9.748945E-01,6.111520E-01,2.937607E-02,-6.551941E-01,9.850497E-01,-8.735961E-01,3.736984E-01,2.904729E-01,-8.252241E-01,9.972748E-01,-7.343759E-01,1.537652E-01,4.917994E-01,-9.249301E-01,9.624481E-01,-5.914507E-01,-2.832721E-02,6.347481E-01,-9.746604E-01,9.089323E-01,-4.678649E-01,-1.643835E-01,7.279335E-01,-9.942639E-01,8.580487E-01,-3.772238E-01,-2.532403E-01,7.814692E-01,-9.994006E-01,8.238140E-01,-3.265270E-01,-2.964915E-01,8.033052E-01,-9.999680E-01,8.138006E-01,-3.187057E-01,-2.956145E-01,7.972494E-01,-9.996279E-01,8.300094E-01,-3.541520E-01,-2.505747E-01,7.622089E-01,-9.957278E-01,8.691561E-01,-4.309609E-01,-1.598536E-01,6.923968E-01,-9.793524E-01,9.222145E-01,-5.438468E-01,-2.190231E-02,5.785796E-01,-9.356979E-01,9.734734E-01,-6.819152E-01,1.619248E-01,4.107348E-01,-8.454057E-01,9.998241E-01,-8.258163E-01,3.830469E-01,1.825966E-01,-6.878406E-01,9.715185E-01,-9.454018E-01,6.205555E-01,-1.017366E-01,-4.473400E-01,8.560891E-01,-9.998216E-01,8.369663E-01,-4.199219E-01,-1.228500E-01,6.271161E-01,-9.427203E-01,9.778411E-01,-7.247782E-01,2.603390E-01,2.784016E-01,-7.350877E-01,9.794319E-01,-9.438090E-01,6.410108E-01,-1.588177E-01,-3.662653E-01,7.880538E-01,-9.912555E-01,9.223666E-01,-6.028132E-01,1.214180E-01,3.910289E-01,-7.976451E-01,9.918675E-01,-9.247804E-01,6.165362E-01,-1.494869E-01,-3.545200E-01,7.662692E-01,-9.821918E-01,9.498824E-01,-6.799942E-01,2.420576E-01,2.541284E-01,-6.864112E-01,9.503325E-01,-9.839456E-01,7.816328E-01,-3.940448E-01,-8.528275E-02,5.429659E-01,-8.727010E-01,9.996829E-01,-8.969887E-01,5.904984E-01,-1.516913E-01,-3.195758E-01,7.180569E-01,-9.565396E-01,9.845256E-01,-7.982775E-01,4.404055E-01,1.049277E-02,-4.575163E-01,8.064188E-01,-9.852973E-01,9.589537E-01,-7.351766E-01,3.619898E-01,8.319162E-02,-5.099624E-01,8.333594E-01,-9.905675E-01,9.526726E-01,-7.293707E-01,3.660212E-01,6.605752E-02,-4.838993E-01,8.088936E-01,-9.813945E-01,9.712693E-01,-7.826136E-01,4.520260E-01,-4.104218E-02,-3.756441E-01,7.238739E-01,-9.431134E-01,9.966633E-01,-8.773359E-01,6.078245E-01,-2.359443E-01,-1.742079E-01,5.534964E-01,-8.393985E-01,9.861148E-01,-9.715495E-01,8.001695E-01,-5.014759E-01,1.245420E-01,2.703294E-01,-6.213780E-01,8.749967E-01,-9.937038E-01,9.613364E-01,-7.848132E-01,4.923703E-01,-1.287054E-01,-2.520981E-01,5.947244E-01,-8.506190E-01,9.846860E-01,-9.798007E-01,8.386134E-01,-5.825310E-01,2.481614E-01,1.181626E-01,-4.669373E-01,7.521856E-01,-9.373990E-01,9.999810E-01,-9.336932E-01,7.488920E-01,-4.706447E-01,1.350762E-01,2.155005E-01,-5.380000E-01,7.938322E-01,-9.533877E-01,9.992464E-01,-9.277952E-01,7.491527E-01,-4.855054E-01,1.681343E-01,1.664532E-01,-4.807971E-01,7.406648E-01,-9.186773E-01,9.970174E-01,-9.689786E-01,8.392559E-01,-6.230209E-01,3.439504E-01,-3.147369E-02,-2.824304E-01,5.665359E-01,-7.934332E-01,9.420621E-01,-9.995004E-01,9.618763E-01,-8.343729E-01,6.303799E-01,-3.699239E-01,7.756991E-02,2.199810E-01,-4.963607E-01,7.278435E-01,-8.953008E-01,9.856536E-01,-9.927283E-01,9.174815E-01,-7.676159E-01,5.566603E-01,-3.026233E-01,2.636051E-02,2.501938E-01,-5.057818E-01,7.214261E-01,-8.817666E-01,9.760493E-01,-9.987120E-01,9.495539E-01,-8.335044E-01,6.600358E-01,-4.422887E-01};

float AUDIO_PULSE_USED_TO_FILTER[] = {-2.456169E-02,2.196005E-01,-4.067566E-01,5.786331E-01,-7.283136E-01,8.496487E-01,-9.375208E-01,9.880747E-01,-9.989034E-01,9.691798E-01,-8.997268E-01,7.930209E-01,-6.531263E-01,4.855606E-01,-2.970941E-01,9.549075E-02,1.108020E-01,-3.130014E-01,5.023618E-01,-6.705573E-01,8.100553E-01,-9.144667E-01,9.788546E-01,-9.999863E-01,9.765160E-01,-9.090870E-01,8.003449E-01,-6.548607E-01,4.789615E-01,-2.804753E-01,6.839869E-02,1.474999E-01,-3.571242E-01,5.505221E-01,-7.183633E-01,8.524005E-01,-9.458900E-01,9.939509E-01,-9.938425E-01,9.451436E-01,-8.498213E-01,7.121834E-01,-5.387107E-01,3.377763E-01,-1.192611E-01,-1.059181E-01,3.263457E-01,-5.306844E-01,7.082638E-01,-8.496487E-01,9.471580E-01,-9.953044E-01,9.911285E-01,-9.344074E-01,8.277207E-01,-6.763679E-01,4.881354E-01,-2.729217E-01,4.223779E-02,1.913948E-01,-4.151171E-01,6.164412E-01,-7.839565E-01,9.079923E-01,-9.811984E-01,9.990081E-01,-9.599531E-01,8.658070E-01,-7.215456E-01,5.351187E-01,-3.170424E-01,7.983037E-02,1.627070E-01,-3.962580E-01,6.068542E-01,-7.817148E-01,9.100397E-01,-9.837016E-01,9.977906E-01,-9.509755E-01,8.456521E-01,-6.878634E-01,4.869915E-01,-2.552318E-01,6.877911E-03,2.425433E-01,-4.772355E-01,6.821367E-01,-8.438994E-01,9.517826E-01,-9.983952E-01,9.802384E-01,-8.980045E-01,7.566069E-01,-5.649298E-01,3.353089E-01,-8.276831E-02,-1.759414E-01,4.234426E-01,-6.428991E-01,8.191729E-01,-9.398916E-01,9.963485E-01,-9.841693E-01,9.036915E-01,-7.600230E-01,5.627658E-01,-3.254168E-01,6.447711E-02,2.016711E-01,-4.540489E-01,6.744357E-01,-8.466993E-01,9.580034E-01,-9.998004E-01,9.685310E-01,-8.659708E-01,6.991889E-01,-4.801111E-01,2.247099E-01,4.812716E-02,-3.179741E-01,5.643891E-01,-7.684717E-01,9.143341E-01,-9.903731E-01,9.902366E-01,-9.134033E-01,7.653188E-01,-5.570671E-01,3.045905E-01,-2.750839E-02,-2.523807E-01,5.128029E-01,-7.327866E-01,8.943718E-01,-9.841112E-01,9.942352E-01,-9.233774E-01,7.767886E-01,-5.660103E-01,3.080201E-01,-2.390684E-02,-2.628241E-01,5.281837E-01,-7.497124E-01,9.084035E-01,-9.903731E-01,9.881250E-01,-9.012934E-01,7.367854E-01,-5.082970E-01,2.352284E-01,5.891985E-02,-3.485434E-01,6.081550E-01,-8.146393E-01,9.493418E-01,-9.998004E-01,9.609562E-01,-8.357210E-01,6.348362E-01,-3.760166E-01,8.244191E-02,2.192810E-01,-5.015120E-01,7.381127E-01,-9.068913E-01,9.917275E-01,-9.841693E-01,8.843412E-01,-7.010598E-01,4.511283E-01,-1.578577E-01,-1.510622E-01,4.461522E-01,-6.989547E-01,8.847995E-01,-9.852531E-01,9.900070E-01,-8.980045E-01,7.176794E-01,-4.662617E-01,1.681981E-01,1.471759E-01,-4.484956E-01,7.054838E-01,-8.920154E-01,9.888192E-01,-9.855320E-01,8.818827E-01,-6.878634E-01,4.228491E-01,-1.137311E-01,-2.077622E-01,5.082970E-01,-7.563927E-01,9.257484E-01,-9.980847E-01,9.651873E-01,-8.299197E-01,6.060729E-01,-3.170424E-01,-6.550397E-03,3.300582E-01,-6.185022E-01,8.403670E-01,-9.710863E-01,9.958279E-01,-9.112577E-01,7.260654E-01,-4.601662E-01,1.426390E-01,1.913948E-01,-5.046257E-01,7.617232E-01,-9.333533E-01,9.995847E-01,-9.522837E-01,7.962024E-01,-5.486067E-01,2.374561E-01,1.016833E-01,-4.296635E-01,7.082638E-01,-9.046708E-01,9.953990E-01,-9.691798E-01,8.284551E-01,-5.892681E-01,2.795320E-01,6.415026E-02,-4.007638E-01,6.897629E-01,-8.959776E-01,9.939509E-01,-9.711645E-01,8.297370E-01,-5.863535E-01,2.704000E-01,7.950389E-02,-4.201761E-01,7.091880E-01,-9.101754E-01,9.974754E-01,-9.594930E-01,8.003449E-01,-5.395382E-01,2.096840E-01,1.474999E-01,-4.864193E-01,7.634182E-01,-9.424372E-01,9.997803E-01,-9.273501E-01,7.339000E-01,-4.440992E-01,9.549075E-02,2.662984E-01,-5.934939E-01,8.424908E-01,-9.797167E-01,9.862447E-01,-8.605152E-01,6.187595E-01,-2.930259E-01,-7.297253E-02,4.296635E-01,-7.283136E-01,9.277173E-01,-9.999549E-01,9.344074E-01,-7.394371E-01,4.414562E-01,-8.146265E-02,-2.905197E-01,6.223551E-01,-8.671151E-01,9.897748E-01,-9.723240E-01,8.165344E-01,-5.439430E-01,1.930019E-01,1.862487E-01,-5.392624E-01,8.148292E-01,-9.725531E-01,9.888680E-01,-8.606820E-01,6.060729E-01,-2.618759E-01,-1.215370E-01,4.875636E-01,-7.817148E-01,9.597693E-01,-9.945128E-01,8.800229E-01,-6.328096E-01,2.895794E-01,9.809863E-02,-4.714689E-01,7.734777E-01,-9.575325E-01,9.947835E-01,-8.787754E-01,6.267036E-01,-2.770153E-01,-1.163339E-01,4.921321E-01,-7.914220E-01,9.667962E-01,-9.900070E-01,8.566533E-01,-5.871491E-01,2.237524E-01,1.759414E-01,-5.480589E-01,8.326505E-01,-9.834058E-01,9.753013E-01,-8.089015E-01,5.105517E-01,-1.283611E-01,-2.754415E-01,6.345831E-01,-8.896345E-01,9.979192E-01,-9.407831E-01,7.269656E-01,-3.914412E-01,-1.015301E-02,4.106425E-01,-7.425163E-01,9.493418E-01,-9.954615E-01,8.722879E-01,-6.000640E-01,2.247099E-01,1.897872E-01,-5.722044E-01,8.563152E-01,-9.924268E-01,9.562004E-01,-7.531700E-01,4.180945E-01,-9.170492E-03,-4.019638E-01,7.427356E-01,-9.524834E-01,9.933617E-01,-8.573284E-01,5.678989E-01,-1.762638E-01,-2.476238E-01,6.274690E-01,-8.943718E-01,9.994087E-01,-9.227475E-01,6.775732E-01,-3.080201E-01,-1.186107E-01,5.240057E-01,-8.331942E-01,9.884747E-01,-9.603193E-01,7.531700E-01,-4.049606E-01,-1.965007E-02,4.411623E-01,-7.800781E-01,9.719401E-01,-9.797167E-01,8.011297E-01,-4.694459E-01,4.747287E-02,3.841959E-01,-7.425163E-01,9.580034E-01,-9.883255E-01,8.268006E-01,-5.040600E-01,8.244191E-02,3.559002E-01,-7.249384E-01,9.516821E-01,-9.907324E-01,8.335563E-01,-5.105517E-01,8.537921E-02,3.574301E-01,-7.292107E-01,9.549455E-01,-9.885243E-01,8.223536E-01,-4.892784E-01,5.630402E-02,3.887270E-01,-7.548910E-01,9.667962E-01,-9.802384E-01,7.916222E-01,-4.391037E-01,-4.912813E-03,4.484956E-01,-7.991652E-01,9.832270E-01,-9.614080E-01,7.374494E-01,-3.577359E-01,-9.809863E-02,5.337345E-01,-8.564843E-01,9.970896E-01,-9.247545E-01,6.541178E-01,-2.425433E-01,-2.218366E-01,6.388764E-01,-9.180094E-01,9.980234E-01,-8.606820E-01,5.351187E-01,-9.190386E-02,-3.720678E-01,7.546762E-01,-9.710863E-01,9.727053E-01,-7.583175E-01,3.748023E-01,9.288222E-02,-5.403653E-01,8.671151E-01,-9.991076E-01,9.057842E-01,-6.073747E-01,1.707804E-01,3.052143E-01,-7.121834E-01,9.567738E-01,-9.822578E-01,7.819190E-01,-4.010639E-01,-7.297253E-02,5.306844E-01,-8.656430E-01,9.993273E-01,-8.997268E-01,5.892681E-01,-1.400451E-01,-3.427040E-01,7.451431E-01,-9.717085E-01,9.679578E-01,-7.339000E-01,3.244875E-01,1.630301E-01,-6.120475E-01,9.144667E-01,-9.968346E-01,8.384088E-01,-4.766598E-01,-1.310088E-03,4.795364E-01,-8.412535E-01,9.974754E-01,-9.090870E-01,5.969153E-01,-1.371259E-01,-3.571242E-01,7.634182E-01,-9.804323E-01,9.533771E-01,-6.881011E-01,2.501616E-01,2.511128E-01,-6.897629E-01,9.548482E-01,-9.786531E-01,7.542462E-01,-3.377763E-01,-1.652917E-01,6.267036E-01,-9.280836E-01,9.914306E-01,-7.995588E-01,4.010639E-01,1.016833E-01,-5.786331E-01,9.056454E-01,-9.969122E-01,8.277207E-01,-4.414562E-01,-6.120836E-02,5.483328E-01,-8.912740E-01,9.987611E-01,-8.414306E-01,4.601662E-01,4.420109E-02,-5.373303E-01,8.870780E-01,-9.990081E-01,8.421378E-01,-4.578383E-01,-5.074413E-02,5.461398E-01,-8.936381E-01,9.980234E-01,-8.299197E-01,4.343895E-01,8.080977E-02,-5.743512E-01,9.100397E-01,-9.945128E-01,8.034757E-01,-3.890287E-01,-1.342054E-01,6.205590E-01,-9.338228E-01,9.855320E-01,-7.602358E-01,3.204572E-01,2.103245E-01,-6.821367E-01,9.608655E-01,-9.665447E-01,6.966087E-01,-2.272623E-01,-3.077084E-01,7.548910E-01,-9.852531E-01,9.315786E-01,-6.084149E-01,1.085232E-01,4.234426E-01,-8.326505E-01,9.992148E-01,-8.735661E-01,4.915618E-01,3.536502E-02,-5.524346E-01,9.068913E-01,-9.932482E-01,7.849721E-01,-3.430117E-01,-2.016711E-01,6.866739E-01,-9.665447E-01,9.565831E-01,-6.588121E-01,1.620606E-01,3.841959E-01,-8.146393E-01,9.981847E-01,-8.781495E-01,4.901350E-01,4.812716E-02,-5.722044E-01,9.210982E-01,-9.868338E-01,7.481932E-01,-2.779593E-01,-2.789030E-01,7.497124E-01,-9.876165E-01,9.176193E-01,-5.605980E-01,2.750839E-02,5.147698E-01,-8.955408E-01,9.941294E-01,-7.784360E-01,3.157996E-01,2.476238E-01,-7.327866E-01,9.846303E-01,-9.218615E-01,5.635778E-01,-2.390684E-02,-5.240057E-01,9.034108E-01,-9.910849E-01,7.576766E-01,-2.779593E-01,-2.927127E-01,7.684717E-01,-9.934743E-01,8.931967E-01,-4.995272E-01,-5.891985E-02,5.984908E-01,-9.407831E-01,9.717858E-01,-6.802185E-01,1.620606E-01,4.106425E-01,-8.466993E-01,9.996568E-01,-8.172900E-01,3.598759E-01,2.192810E-01};

float AUDIO_PULSE_USED[] = {2.192810E-01,3.598759E-01,-8.172900E-01,9.996568E-01,-8.466993E-01,4.106425E-01,1.620606E-01,-6.802185E-01,9.717858E-01,-9.407831E-01,5.984908E-01,-5.891985E-02,-4.995272E-01,8.931967E-01,-9.934743E-01,7.684717E-01,-2.927127E-01,-2.779593E-01,7.576766E-01,-9.910849E-01,9.034108E-01,-5.240057E-01,-2.390684E-02,5.635778E-01,-9.218615E-01,9.846303E-01,-7.327866E-01,2.476238E-01,3.157996E-01,-7.784360E-01,9.941294E-01,-8.955408E-01,5.147698E-01,2.750839E-02,-5.605980E-01,9.176193E-01,-9.876165E-01,7.497124E-01,-2.789030E-01,-2.779593E-01,7.481932E-01,-9.868338E-01,9.210982E-01,-5.722044E-01,4.812716E-02,4.901350E-01,-8.781495E-01,9.981847E-01,-8.146393E-01,3.841959E-01,1.620606E-01,-6.588121E-01,9.565831E-01,-9.665447E-01,6.866739E-01,-2.016711E-01,-3.430117E-01,7.849721E-01,-9.932482E-01,9.068913E-01,-5.524346E-01,3.536502E-02,4.915618E-01,-8.735661E-01,9.992148E-01,-8.326505E-01,4.234426E-01,1.085232E-01,-6.084149E-01,9.315786E-01,-9.852531E-01,7.548910E-01,-3.077084E-01,-2.272623E-01,6.966087E-01,-9.665447E-01,9.608655E-01,-6.821367E-01,2.103245E-01,3.204572E-01,-7.602358E-01,9.855320E-01,-9.338228E-01,6.205590E-01,-1.342054E-01,-3.890287E-01,8.034757E-01,-9.945128E-01,9.100397E-01,-5.743512E-01,8.080977E-02,4.343895E-01,-8.299197E-01,9.980234E-01,-8.936381E-01,5.461398E-01,-5.074413E-02,-4.578383E-01,8.421378E-01,-9.990081E-01,8.870780E-01,-5.373303E-01,4.420109E-02,4.601662E-01,-8.414306E-01,9.987611E-01,-8.912740E-01,5.483328E-01,-6.120836E-02,-4.414562E-01,8.277207E-01,-9.969122E-01,9.056454E-01,-5.786331E-01,1.016833E-01,4.010639E-01,-7.995588E-01,9.914306E-01,-9.280836E-01,6.267036E-01,-1.652917E-01,-3.377763E-01,7.542462E-01,-9.786531E-01,9.548482E-01,-6.897629E-01,2.511128E-01,2.501616E-01,-6.881011E-01,9.533771E-01,-9.804323E-01,7.634182E-01,-3.571242E-01,-1.371259E-01,5.969153E-01,-9.090870E-01,9.974754E-01,-8.412535E-01,4.795364E-01,-1.310088E-03,-4.766598E-01,8.384088E-01,-9.968346E-01,9.144667E-01,-6.120475E-01,1.630301E-01,3.244875E-01,-7.339000E-01,9.679578E-01,-9.717085E-01,7.451431E-01,-3.427040E-01,-1.400451E-01,5.892681E-01,-8.997268E-01,9.993273E-01,-8.656430E-01,5.306844E-01,-7.297253E-02,-4.010639E-01,7.819190E-01,-9.822578E-01,9.567738E-01,-7.121834E-01,3.052143E-01,1.707804E-01,-6.073747E-01,9.057842E-01,-9.991076E-01,8.671151E-01,-5.403653E-01,9.288222E-02,3.748023E-01,-7.583175E-01,9.727053E-01,-9.710863E-01,7.546762E-01,-3.720678E-01,-9.190386E-02,5.351187E-01,-8.606820E-01,9.980234E-01,-9.180094E-01,6.388764E-01,-2.218366E-01,-2.425433E-01,6.541178E-01,-9.247545E-01,9.970896E-01,-8.564843E-01,5.337345E-01,-9.809863E-02,-3.577359E-01,7.374494E-01,-9.614080E-01,9.832270E-01,-7.991652E-01,4.484956E-01,-4.912813E-03,-4.391037E-01,7.916222E-01,-9.802384E-01,9.667962E-01,-7.548910E-01,3.887270E-01,5.630402E-02,-4.892784E-01,8.223536E-01,-9.885243E-01,9.549455E-01,-7.292107E-01,3.574301E-01,8.537921E-02,-5.105517E-01,8.335563E-01,-9.907324E-01,9.516821E-01,-7.249384E-01,3.559002E-01,8.244191E-02,-5.040600E-01,8.268006E-01,-9.883255E-01,9.580034E-01,-7.425163E-01,3.841959E-01,4.747287E-02,-4.694459E-01,8.011297E-01,-9.797167E-01,9.719401E-01,-7.800781E-01,4.411623E-01,-1.965007E-02,-4.049606E-01,7.531700E-01,-9.603193E-01,9.884747E-01,-8.331942E-01,5.240057E-01,-1.186107E-01,-3.080201E-01,6.775732E-01,-9.227475E-01,9.994087E-01,-8.943718E-01,6.274690E-01,-2.476238E-01,-1.762638E-01,5.678989E-01,-8.573284E-01,9.933617E-01,-9.524834E-01,7.427356E-01,-4.019638E-01,-9.170492E-03,4.180945E-01,-7.531700E-01,9.562004E-01,-9.924268E-01,8.563152E-01,-5.722044E-01,1.897872E-01,2.247099E-01,-6.000640E-01,8.722879E-01,-9.954615E-01,9.493418E-01,-7.425163E-01,4.106425E-01,-1.015301E-02,-3.914412E-01,7.269656E-01,-9.407831E-01,9.979192E-01,-8.896345E-01,6.345831E-01,-2.754415E-01,-1.283611E-01,5.105517E-01,-8.089015E-01,9.753013E-01,-9.834058E-01,8.326505E-01,-5.480589E-01,1.759414E-01,2.237524E-01,-5.871491E-01,8.566533E-01,-9.900070E-01,9.667962E-01,-7.914220E-01,4.921321E-01,-1.163339E-01,-2.770153E-01,6.267036E-01,-8.787754E-01,9.947835E-01,-9.575325E-01,7.734777E-01,-4.714689E-01,9.809863E-02,2.895794E-01,-6.328096E-01,8.800229E-01,-9.945128E-01,9.597693E-01,-7.817148E-01,4.875636E-01,-1.215370E-01,-2.618759E-01,6.060729E-01,-8.606820E-01,9.888680E-01,-9.725531E-01,8.148292E-01,-5.392624E-01,1.862487E-01,1.930019E-01,-5.439430E-01,8.165344E-01,-9.723240E-01,9.897748E-01,-8.671151E-01,6.223551E-01,-2.905197E-01,-8.146265E-02,4.414562E-01,-7.394371E-01,9.344074E-01,-9.999549E-01,9.277173E-01,-7.283136E-01,4.296635E-01,-7.297253E-02,-2.930259E-01,6.187595E-01,-8.605152E-01,9.862447E-01,-9.797167E-01,8.424908E-01,-5.934939E-01,2.662984E-01,9.549075E-02,-4.440992E-01,7.339000E-01,-9.273501E-01,9.997803E-01,-9.424372E-01,7.634182E-01,-4.864193E-01,1.474999E-01,2.096840E-01,-5.395382E-01,8.003449E-01,-9.594930E-01,9.974754E-01,-9.101754E-01,7.091880E-01,-4.201761E-01,7.950389E-02,2.704000E-01,-5.863535E-01,8.297370E-01,-9.711645E-01,9.939509E-01,-8.959776E-01,6.897629E-01,-4.007638E-01,6.415026E-02,2.795320E-01,-5.892681E-01,8.284551E-01,-9.691798E-01,9.953990E-01,-9.046708E-01,7.082638E-01,-4.296635E-01,1.016833E-01,2.374561E-01,-5.486067E-01,7.962024E-01,-9.522837E-01,9.995847E-01,-9.333533E-01,7.617232E-01,-5.046257E-01,1.913948E-01,1.426390E-01,-4.601662E-01,7.260654E-01,-9.112577E-01,9.958279E-01,-9.710863E-01,8.403670E-01,-6.185022E-01,3.300582E-01,-6.550397E-03,-3.170424E-01,6.060729E-01,-8.299197E-01,9.651873E-01,-9.980847E-01,9.257484E-01,-7.563927E-01,5.082970E-01,-2.077622E-01,-1.137311E-01,4.228491E-01,-6.878634E-01,8.818827E-01,-9.855320E-01,9.888192E-01,-8.920154E-01,7.054838E-01,-4.484956E-01,1.471759E-01,1.681981E-01,-4.662617E-01,7.176794E-01,-8.980045E-01,9.900070E-01,-9.852531E-01,8.847995E-01,-6.989547E-01,4.461522E-01,-1.510622E-01,-1.578577E-01,4.511283E-01,-7.010598E-01,8.843412E-01,-9.841693E-01,9.917275E-01,-9.068913E-01,7.381127E-01,-5.015120E-01,2.192810E-01,8.244191E-02,-3.760166E-01,6.348362E-01,-8.357210E-01,9.609562E-01,-9.998004E-01,9.493418E-01,-8.146393E-01,6.081550E-01,-3.485434E-01,5.891985E-02,2.352284E-01,-5.082970E-01,7.367854E-01,-9.012934E-01,9.881250E-01,-9.903731E-01,9.084035E-01,-7.497124E-01,5.281837E-01,-2.628241E-01,-2.390684E-02,3.080201E-01,-5.660103E-01,7.767886E-01,-9.233774E-01,9.942352E-01,-9.841112E-01,8.943718E-01,-7.327866E-01,5.128029E-01,-2.523807E-01,-2.750839E-02,3.045905E-01,-5.570671E-01,7.653188E-01,-9.134033E-01,9.902366E-01,-9.903731E-01,9.143341E-01,-7.684717E-01,5.643891E-01,-3.179741E-01,4.812716E-02,2.247099E-01,-4.801111E-01,6.991889E-01,-8.659708E-01,9.685310E-01,-9.998004E-01,9.580034E-01,-8.466993E-01,6.744357E-01,-4.540489E-01,2.016711E-01,6.447711E-02,-3.254168E-01,5.627658E-01,-7.600230E-01,9.036915E-01,-9.841693E-01,9.963485E-01,-9.398916E-01,8.191729E-01,-6.428991E-01,4.234426E-01,-1.759414E-01,-8.276831E-02,3.353089E-01,-5.649298E-01,7.566069E-01,-8.980045E-01,9.802384E-01,-9.983952E-01,9.517826E-01,-8.438994E-01,6.821367E-01,-4.772355E-01,2.425433E-01,6.877911E-03,-2.552318E-01,4.869915E-01,-6.878634E-01,8.456521E-01,-9.509755E-01,9.977906E-01,-9.837016E-01,9.100397E-01,-7.817148E-01,6.068542E-01,-3.962580E-01,1.627070E-01,7.983037E-02,-3.170424E-01,5.351187E-01,-7.215456E-01,8.658070E-01,-9.599531E-01,9.990081E-01,-9.811984E-01,9.079923E-01,-7.839565E-01,6.164412E-01,-4.151171E-01,1.913948E-01,4.223779E-02,-2.729217E-01,4.881354E-01,-6.763679E-01,8.277207E-01,-9.344074E-01,9.911285E-01,-9.953044E-01,9.471580E-01,-8.496487E-01,7.082638E-01,-5.306844E-01,3.263457E-01,-1.059181E-01,-1.192611E-01,3.377763E-01,-5.387107E-01,7.121834E-01,-8.498213E-01,9.451436E-01,-9.938425E-01,9.939509E-01,-9.458900E-01,8.524005E-01,-7.183633E-01,5.505221E-01,-3.571242E-01,1.474999E-01,6.839869E-02,-2.804753E-01,4.789615E-01,-6.548607E-01,8.003449E-01,-9.090870E-01,9.765160E-01,-9.999863E-01,9.788546E-01,-9.144667E-01,8.100553E-01,-6.705573E-01,5.023618E-01,-3.130014E-01,1.108020E-01,9.549075E-02,-2.970941E-01,4.855606E-01,-6.531263E-01,7.930209E-01,-8.997268E-01,9.691798E-01,-9.989034E-01,9.880747E-01,-9.375208E-01,8.496487E-01,-7.283136E-01,5.786331E-01,-4.067566E-01,2.196005E-01,-2.456169E-02};


float AUDIO_PRE_FILTER_A[] = {1.000000E+00,3.743862E+00,8.319277E+00,1.266820E+01,1.459382E+01,1.314507E+01,9.486138E+00,5.530287E+00,2.612640E+00,9.948741E-01,3.018292E-01,7.140343E-02,1.271896E-02,1.606694E-03,1.284674E-04,4.892788E-06};

float AUDIO_PRE_FILTER_B[] = {5.225986E-06,-7.838978E-05,5.487285E-04,-2.377823E-03,7.133470E-03,-1.569363E-02,2.615606E-02,-3.362922E-02,3.362922E-02,-2.615606E-02,1.569363E-02,-7.133470E-03,2.377823E-03,-5.487285E-04,7.838978E-05,-5.225986E-06};
