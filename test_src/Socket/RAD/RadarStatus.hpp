
#include "MAS_TFC_MasterAgent.hpp"
#include "SaveScale.hpp"

class RadarStatus: public MAS::TFC::MAF::ActiveClass {
  static RadarStatus *_instance;
  static RadarStatus *getInstance( void);
  SaveScale saveScale;
  short _previousScale;
  void _saveScale( const short scale);
}
