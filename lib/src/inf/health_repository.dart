import 'package:health/health.dart';

class HealthRepository {
  final health = HealthFactory();

  Future<bool> getBloodOxygen() async {
    bool request = await health.requestAuthorization([HealthDataType.BLOOD_OXYGEN]);

    if (request) {
      List<HealthDataPoint> healthData = await health.getHealthDataFromTypes(
          DateTime.now().subtract(const Duration(days: 7)),
          DateTime.now(),
          [HealthDataType.BLOOD_OXYGEN]);

      return healthData.isNotEmpty;
    }
    return false;
  }
}
