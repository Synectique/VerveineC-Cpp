
#include "MAS_TFC_MasterAgent.hpp"
#include "RadarStatus.hpp"

class VideoProcessing:public MAS::TFC::MAF::ActiveClass {
  VideoProcessing( void) { }
  protected:void _setVideoScale(const short range) {
    RadarStatus::getInstance()->saveScale( range);
  }
}
