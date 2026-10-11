package com.codesbyfebin.seekerutility
import kotlin.math.*
import java.util.PriorityQueue

data class MapPoint(val lat:Double,val lon:Double)
object RoadRouter {
 fun distance(a:MapPoint,b:MapPoint):Double{val dLat=Math.toRadians(b.lat-a.lat);val dLon=Math.toRadians(b.lon-a.lon);val h=sin(dLat/2).pow(2)+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*sin(dLon/2).pow(2);return 6371000*2*asin(sqrt(h.coerceIn(0.0,1.0)))}
 fun route(roads:List<List<MapPoint>>,start:MapPoint,end:MapPoint):List<MapPoint>{
  val graph=mutableMapOf<MapPoint,MutableSet<MapPoint>>();for(road in roads)for(i in 1 until road.size){val a=road[i-1];val b=road[i];graph.getOrPut(a){mutableSetOf()}.add(b);graph.getOrPut(b){mutableSetOf()}.add(a)};require(graph.isNotEmpty() && graph.size<=30000)
  val source=graph.keys.minBy{distance(it,start)};val destination=graph.keys.minBy{distance(it,end)};require(distance(source,start)<300 && distance(destination,end)<300){"Endpoints must be within 300 m of cached roads"}
  val cost=mutableMapOf(source to 0.0);val previous=mutableMapOf<MapPoint,MapPoint>();val queue=PriorityQueue<Pair<MapPoint,Double>>(compareBy{it.second});queue.add(source to 0.0)
  while(queue.isNotEmpty()){val (point,weight)=queue.remove();if(weight!=cost[point])continue;if(point==destination)break;for(next in graph.getValue(point)){val total=weight+distance(point,next);if(total<(cost[next]?:Double.POSITIVE_INFINITY)){cost[next]=total;previous[next]=point;queue.add(next to total)}}}
  require(destination in cost){"No connected road path"};val path=mutableListOf(destination);while(path.last()!=source)path.add(previous.getValue(path.last()));return path.reversed()
 }
}
