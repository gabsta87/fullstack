import { Component, OnInit } from '@angular/core';
import {WorkerProfile} from "../../models/worker.model";
import {IonicModule} from "@ionic/angular";
import {NgForOf} from "@angular/common";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-main-gallery',
  templateUrl: './main-gallery.component.html',
  styleUrls: ['./main-gallery.component.scss'],
  standalone: true,
  imports: [IonicModule, NgForOf],
})
export class MainGalleryComponent  implements OnInit {

  workers!:WorkerProfile[];

  constructor(private route: ActivatedRoute, private router : Router) {}

  ngOnInit(): void {
    this.workers = this.route.snapshot.data['profiles'];
    console.log(this.workers);
  }

  goToProfile(id: number) {
    this.router.navigate(['/profile'],{queryParams:{id:id}});
  }

}
