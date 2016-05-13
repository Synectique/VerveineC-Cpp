
#include "SaveScale.hpp"
#include "RadarStatus.hpp"

SaveScale::SaveScale( RadarStatus * radarStatus) :
	Service1<RadarStatus, const short> ( radarStatus, __FILE__, &RadarStatus::_saveScale)
{
}
