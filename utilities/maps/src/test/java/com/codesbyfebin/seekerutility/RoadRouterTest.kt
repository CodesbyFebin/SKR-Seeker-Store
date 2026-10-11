package com.codesbyfebin.seekerutility
import org.junit.Test
import org.junit.Assert.*
class RoadRouterTest {
 @Test fun connectedRoads(){val a=MapPoint(9.0,76.0);val b=MapPoint(9.001,76.0);val c=MapPoint(9.002,76.0);assertEquals(listOf(a,b,c),RoadRouter.route(listOf(listOf(a,b),listOf(b,c)),a,c));assertTrue(RoadRouter.distance(a,b)>100)}
 @Test fun rejectDisconnected(){val a=MapPoint(9.0,76.0);val b=MapPoint(9.001,76.0);val c=MapPoint(9.02,76.0);val d=MapPoint(9.021,76.0);try{RoadRouter.route(listOf(listOf(a,b),listOf(c,d)),a,d);fail()}catch(_:IllegalArgumentException){}}
}
