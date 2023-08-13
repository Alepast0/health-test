import 'package:health/health.dart';
import 'package:health_test/src/inf/health_repository.dart';

class HomeController{
  // final rep = HealthRepository();
  //
  // Future<void> getData() async{
  //   rep.getBloodOxygen();
  // }
  Future<void> getData() async{
    final health = HealthFactory(useHealthConnectIfAvailable: true);

    var types = [
      HealthDataType.STEPS,
      HealthDataType.BLOOD_GLUCOSE,
    ];

    bool requested = await health.requestAuthorization(types);

    var now = DateTime.now();

    List<HealthDataPoint> healthData = await health.getHealthDataFromTypes(
        now.subtract(Duration(days: 1)), now, types);

    types = [HealthDataType.STEPS, HealthDataType.BLOOD_GLUCOSE];
    var permissions = [
      HealthDataAccess.READ_WRITE,
      HealthDataAccess.READ_WRITE
    ];
    await health.requestAuthorization(types, permissions: permissions);

    bool success = await health.writeHealthData(10, HealthDataType.STEPS, now, now);
    success = await health.writeHealthData(3.1, HealthDataType.BLOOD_GLUCOSE, now, now);

    var midnight = DateTime(now.year, now.month, now.day);
    int? steps = await health.getTotalStepsInInterval(midnight, now);

    print(steps.toString());
  }
}