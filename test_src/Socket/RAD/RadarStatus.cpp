
#include "RadarStatus.hpp"


RadarStatus::RadarStatus( void) : saveScale( this), _previousScale( 64) {
  RadarStatus::_instance = this;
}
static RadarStatus *RadarStatus::getInstance( void) {
  return RadarStatus::_instance;
}
void RadarStatus::_saveScale( const short scale) {
  _previousScale = scale;
}
