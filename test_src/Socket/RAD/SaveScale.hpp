#include "MAS_TFC_MasterAgent.hpp"

class RadarStatus;

class SaveScale:public MAS::TFC::MAF::Service1 < RadarStatus, const short > {
    public: SaveScale(RadarStatus * radarStatus);
};
