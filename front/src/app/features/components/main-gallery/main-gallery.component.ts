import { Component, OnInit } from '@angular/core';
import {Profile} from "../profile/profile.component";
import {IonicModule} from "@ionic/angular";
import {JsonPipe, NgForOf} from "@angular/common";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-main-gallery',
  templateUrl: './main-gallery.component.html',
  styleUrls: ['./main-gallery.component.scss'],
  standalone: true,
  imports: [IonicModule, NgForOf, JsonPipe],
})
export class MainGalleryComponent  implements OnInit {

  workers!:Profile[];

  constructor(private route: ActivatedRoute, private router : Router) {}

  ngOnInit(): void {
    this.workers = this.route.snapshot.data['profiles'];
    console.log(this.workers);
  }

  goToProfile(id: number) {
    this.router.navigate(['/profile'],{queryParams:{id:id}});
  }

}
