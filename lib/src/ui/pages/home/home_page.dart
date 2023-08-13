import 'package:flutter/material.dart';
import 'package:health_test/src/ui/pages/home/home_controller.dart';
import 'package:permission_handler/permission_handler.dart';

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final controller = HomeController();

  @override
  void initState() {
    getPermiss();
    controller.getData();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("data"),),
      body: Container(color: Colors.pinkAccent,),
    );
  }

  Future<void> getPermiss() async {
    final permissionStatus = Permission.activityRecognition.request();
    if (await permissionStatus.isDenied ||
        await permissionStatus.isPermanentlyDenied) {
      print(
          'activityRecognition permission required to fetch your steps count');
      return;
    }


  }
}
